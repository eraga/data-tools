package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ProcessingContext.asTypeSpec

/**
 * **DTOGenerator**
 *
 * Generates DTO [TypeSpec]s from single model interface
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:02
 */
@KotlinPoetMetadataPreview
class DTOGenerator(
    listOfImplementations: List<DTOSettings>
) : AbstractGenerator<DTOSettings>(listOfImplementations) {
    override fun generate() {
        for (implementation in listOfImplementations) {
            generateImplementation(implementation)
        }
    }

    private fun generateImplementation(impl: DTOSettings) {
        val className = impl.implClassName
        println("           STARTING impl $className")
        val element = impl.modelElement
        val fileBuilder = impl.fileBuilder

        val typeBuilder = TypeSpec
            .classBuilder(className)
            .addModifiers(impl.classModifier)

        val kmClass = element.getAnnotation(Metadata::class.java)!!.toImmutableKmClass()
        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)

        /**
         * DTO Classes don't inherit from model
         */
        fun supersHaveOnlyNullableProps(spec: TypeSpec): Boolean {
            if (spec.superinterfaces.isEmpty()) {
                if (spec.propertySpecs.isEmpty())
                    return true
                else
                    if (spec.propertySpecs.all { it.type.isNullable })
                        return true
                return false
            } else {
                return spec.superinterfaces.all { supersHaveOnlyNullableProps(it.key.asTypeSpec()) }
            }
        }

        var superinterfaces = if (impl.ownSettings.propsForceNull)
            kmClassSpec.superinterfaces.keys.filter { typeName ->
                supersHaveOnlyNullableProps(typeName.asTypeSpec())
            }.toMutableList()
        else
            kmClassSpec.superinterfaces.keys.toMutableList()

        // Force Cloneable
        if (superinterfaces.none { it == Cloneable::class.asTypeName() }) {
            superinterfaces.add(Cloneable::class.asTypeName())
        }

        // Force Comparable
        if (superinterfaces.none { it == Comparable::class.asTypeName() }) {
            superinterfaces.add(Comparable::class.asTypeName().parameterizedBy(impl.implClassName))
        }

        superinterfaces = superinterfaces.map {
            val type = if (it is ParameterizedTypeName) {
                if (it.typeArguments.contains(impl.modelClassName)) {
                    ClassName(it.rawType.packageName, it.rawType.simpleName).parameterizedBy(
                        it.typeArguments.map { arg ->
                            if (arg == impl.modelClassName) impl.implClassName else it
                        }
                    )
                } else {
                    it
                }
            } else {
                it
            }
            type
        }.toMutableList()

        /**
         * Gather properties and create constructor
         */
        val constructorBuilder = FunSpec.constructorBuilder()

        var propertyNum = 0

        val gatheredProperties = gatherProperties(element, className)

        val skippedProperties = gatheredProperties.filter { it.value.preventOverride }

        if (skippedProperties.isNotEmpty()) {
            superinterfaces = correctInheritanceChainFor(skippedProperties, superinterfaces)
        }

        superinterfaces.forEach {
            typeBuilder.addSuperinterface(it)
        }

        for ((_, propertyData) in gatheredProperties) {
            if (propertyData.preventOverride || propertyData.isFinal) {
                continue
            }
            val name = propertyData.propertySpec.name

            val defaultInit = propertyData.defaultInit

            val type = determinePropertyType(element, propertyData, this)
                .let {
                    if (impl.ownSettings.propsForceNull)
                        it.copy(nullable = true)
                    else
                        it
                }


            val kotlinProperty = PropertySpec.builder(name, type)
                .mutable(true)
                .setter(
                    FunSpec.setterBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )



            if (superinterfaces.any {
                    supersHaveThisProp(it.asTypeSpec(), propertyData.propertySpec)
                })
                kotlinProperty.addModifiers(KModifier.OVERRIDE)

            val defaultValue = defaultInit ?: constructorDefaultInitializer(
                impl,
                type,
                propertyData
            )

            val defaultValueLiteral = if (defaultValue.contains("\""))
                "%S"
            else
                "%L"

            val defaultValueValue = if (defaultValue.contains("\""))
                defaultValue.replace("\"", "").replace("\\", "")
            else
                defaultValue


            addAnnotations(propertyData, kotlinProperty, impl)

            if (impl.constructorVarargPosition == propertyNum)
                constructorBuilder.addParameter(
                    ParameterSpec.builder(
                        "skipMe",
                        ProcessingContext.ignoreItClassName
                    )
                        .addAnnotation(SUPPRESS_SKIP_ME)
                        .addModifiers(KModifier.VARARG)
                        .build()
                )

            constructorBuilder.addParameter(
                ParameterSpec.builder(
                    name,
                    type
                )
                    .defaultValue(defaultValueLiteral, defaultValueValue)
                    .build()
            )

            typeBuilder.addProperty(
                kotlinProperty
                    .initializer(defaultValueLiteral, defaultValueValue)
                    .build()
            )
            propertyNum++
        }

        /**
         * No Arg Constructor
         */
        typeBuilder
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )


        typeBuilder
            .addFunction(
                constructorBuilder
                    .addCode(
                        funAllArgsConstructorCode(typeBuilder.propertySpecs)
                            .build()
                    )
                    .callThisConstructor()
                    .build()
            )

        if (impl.implementComparable)
            implementComparable(impl, typeBuilder)

        if (impl.implementCloneable)
            implementCloneable(impl, typeBuilder)

        if (impl.implementHashCode)
            implementHashCode(impl, typeBuilder)

        if (impl.implementEquals)
            implementEquals(impl, typeBuilder)

        if (impl.implementToString)
            implementToString(typeBuilder)

        if (impl.implementCopy)
            implementCopiable(typeBuilder, impl.implClassName)

        impl.typeSpec = typeBuilder.build()

        fileBuilder.addType(impl.typeSpec)

        /**
         * Extension to create DTO instance from any class inheriting model interface
         */
        fileBuilder.addFunction(
            funModelExtensionToBuilder(impl, typeBuilder.propertySpecs)
                .build()
        )
        fileBuilder.addFunction(
            funModelIterableExtensionBuilder(impl, "to")
                .build()
        )

        fileSpecs.add(fileBuilder.build())
    }
}
