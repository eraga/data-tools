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
        val element = impl.modelElement
        val fileBuilder = FileSpec.builder(
                className.packageName,
                className.simpleName)

        val typeBuilder = TypeSpec
                .classBuilder(className)
                .addModifiers(impl.classModifier)

        val kmClass = element.getAnnotation(Metadata::class.java)!!.toImmutableKmClass()
        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)

        kmClassSpec.superinterfaces.keys.first().asTypeSpec().superinterfaces
        /**
         * DTO Classes don't inherit from model
         */
        fun supersHaveOnlyNullableProps(spec: TypeSpec): Boolean {
            if(spec.superinterfaces.isEmpty()) {
                if(spec.propertySpecs.isEmpty())
                    return true
                else
                    if (spec.propertySpecs.all { it.type.isNullable })
                        return true
                return false
            } else {
                return spec.superinterfaces.all { supersHaveOnlyNullableProps(it.key.asTypeSpec()) }
            }
        }

        fun supersHaveThisProp(spec: TypeSpec, propertySpec: PropertySpec): Boolean {
            return if(spec.superinterfaces.isEmpty())
                spec.propertySpecs.contains(propertySpec)
            else
                spec.superinterfaces.any { supersHaveThisProp(it.key.asTypeSpec(), propertySpec) }
        }

        val superinterfaces = if (impl.implementAnnotation.propsForceNull)
            kmClassSpec.superinterfaces.keys.filter { typeName ->
                supersHaveOnlyNullableProps(typeName.asTypeSpec())
            }
        else
            kmClassSpec.superinterfaces.keys

        superinterfaces.forEach {
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
            typeBuilder.addSuperinterface(type)
        }

        /**
         * Gather properties and create constructor
         */
        val constructorBuilder = FunSpec.constructorBuilder()

        var propertyNum = 0
        for ((name, propertyData) in gatherProperties(element)) {
            if (propertyData.preventOverride)
                continue

            val defaultInit = propertyData.defaultInit
            val property = propertyData.typeSpec


            val type = if (impl.implementAnnotation.propsForceNull)
                determinePropertyType(element, propertyData).copy(nullable = true)
            else
                determinePropertyType(element, propertyData)


            val kotlinProperty = PropertySpec.builder(name, type)
                    .mutable(true)
                    .setter(FunSpec.setterBuilder()
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
                    propertyData)

            if (impl.constructorVarargPosition == propertyNum)
                constructorBuilder.addParameter(
                        ParameterSpec.builder(
                                "skipMe",
                                ProcessingContext.ignoreItClassName
                        )
                                .addModifiers(KModifier.VARARG)
                                .build()
                )

            constructorBuilder.addParameter(
                    ParameterSpec.builder(
                            name,
                            type
                    )
                            .defaultValue(defaultValue)
                            .build()
            )

            typeBuilder.addProperty(
                    kotlinProperty
                            .initializer(defaultValue)
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
                .addFunction(constructorBuilder
                        .addCode(
                                funAllArgsConstructorCode(typeBuilder.propertySpecs)
                                        .build()
                        )
                        .callThisConstructor()
                        .build())

        /**
         * Extension to create instance from any instance inheriting model interface
         */
//        val modelConstructorBuilder = funModelConstructorBuilder(
//                impl, typeBuilder.propertySpecs
//        )


        if (impl.implementComparable)
            implementComparable(impl, typeBuilder)

        if (impl.implementHashCode)
            implementHashCode(impl, typeBuilder)

        if (impl.implementEquals)
            implementEquals(impl, typeBuilder)

//      TODO:
//        if(impl.implementToString)
//            implementToString(typeBuilder)


        fileBuilder.addType(typeBuilder.build())

        fileBuilder.addFunction(
                funModelExtensionToBuilder(impl, typeBuilder.propertySpecs)
                        .build())

        fileSpecs.add(fileBuilder.build())
    }
}
