package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toFileSpec
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.model.ProcessingContext.firstImplementation
import net.eraga.tools.model.ProcessingContext.firstImplementationImmutable
import net.eraga.tools.models.Implement

/**
 * **ImmutableGenerator**
 *
 * Generates [Implement.Immutable] [TypeSpec]s from single model interface
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
class ImmutableGenerator(
    listOfImplementations: List<ImmutableSettings>
) : AbstractGenerator<ImmutableSettings>(listOfImplementations) {
    override fun generate() {
        for (implementation in listOfImplementations) {
            generateImplementation(implementation)
        }
    }

    private fun generateImplementation(impl: ImmutableSettings) {
        val className = impl.implClassName
        val element = impl.modelElement
        val fileBuilder = impl.fileBuilder

        val typeBuilder = TypeSpec
            .classBuilder(className)
            .addModifiers(impl.classModifier)

        val kmClass = element.getAnnotation(Metadata::class.java)!!.toImmutableKmClass()
        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)

//        kmClassSpec.superinterfaces.keys.first().asTypeSpec().superinterfaces


        var superinterfaces = mutableListOf<TypeName>(impl.modelClassName)


        /**
         * Gather properties and create constructor
         */
        val constructorBuilder = FunSpec.constructorBuilder()

        var propertyNum = 0
        val gatheredProperties = gatherProperties(
            kmClass.toFileSpec(ProcessingContext.classInspector), impl.implClassName
        )

        val skippedProperties = gatheredProperties.filter { it.value.preventOverride }

        if (skippedProperties.isNotEmpty()) {
            superinterfaces = correctInheritanceChainFor(skippedProperties, superinterfaces)
        }

        superinterfaces.forEach {
            if (it == impl.modelClassName &&
                impl.modelClassName.asTypeSpec().kind == TypeSpec.Kind.CLASS
            ) {
                typeBuilder.superclass(impl.modelClassName)
            } else {
                typeBuilder.addSuperinterface(it)
            }
        }


        /**
         * Class annotations
         */
        kmClassSpec.annotationSpecs
            .filterNot { it.typeName != Implement.Annotate::class.asTypeName() }
            .filter {
                impl.implementAnnotations.toRegex().matches(it.typeName.toString())
            }
            .forEach {
                if (it.typeName.asClassName().canonicalName !in IGNORED_ANNOTATIONS)
                    typeBuilder.addAnnotation(it)
            }

        implementAnnotates(typeBuilder, kmClassSpec, impl.implClassName.simpleName)


        val defaultConstructorProperties = mutableListOf<PropertySpec>()

        for ((_, propertyData) in gatheredProperties) {
            if (propertyData.preventOverride || propertyData.isFinal)
                continue

            val name = propertyData.propertySpec.name

            val defaultInit = propertyData.defaultInit

            val type = determinePropertyType(element, propertyData, this)

            val kotlinProperty = PropertySpec.builder(name, type)
                .mutable(true)

            if (propertyData.propertySpec.setter == null && !propertyData.propertySpec.mutable) {
                kotlinProperty
                    .setter(
                        FunSpec.setterBuilder()
                            .addModifiers(KModifier.PRIVATE)
                            .build()
                    )
            }

//            if(propertyData.propertySpec.name == "type") {
//                println(propertyData.propertySpec.tags.values.filterIsInstance<ImmutableKmProperty>() )
//            }

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
                defaultValue.replace("\"", "")
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

            val constructorParam =
                ParameterSpec.builder(
                    name,
                    type
                )

            if (!propertyData.constructorInit) {
                constructorParam.defaultValue(defaultValueLiteral, defaultValueValue)
                kotlinProperty
                    .initializer(defaultValueLiteral, defaultValueValue)
            } else {
                kotlinProperty.initializer(propertyData.propertySpec.name)
                defaultConstructorProperties.add(kotlinProperty.build())
            }

            typeBuilder.addProperty(
                kotlinProperty
                    .build()
            )

            constructorBuilder.addParameter(
                constructorParam.build()
            )

            propertyNum++
        }

        val code = defaultConstructorProperties
            .map { it.name }
            .joinToString(", ") {
                "$it = $it"
            }
        val defaultConstructorArgs: CodeBlock = CodeBlock.builder().add(code).build()


        /**
         * Primary Constructor
         */
        if (defaultConstructorProperties.isEmpty()) {
            typeBuilder
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
        } else {
            val constructFun = FunSpec
                .constructorBuilder()

            for (prop in defaultConstructorProperties) {
                constructFun.addParameter(prop.name, prop.type)
            }

            typeBuilder
                .primaryConstructor(
                    constructFun
                        .build()
                )
        }

        typeBuilder
            .addFunction(
                constructorBuilder
                    .addCode(
                        funAllArgsConstructorCode(typeBuilder.propertySpecs)
                            .build()
                    )
                    .callThisConstructor(defaultConstructorArgs)
                    .build()
            )

        /**
         * Reimplement secondary constructors
         */
        for (constr in kmClassSpec.funSpecs.filter { it.isConstructor }) {
            val builder = constr.toBuilder()
                .callThisConstructor(defaultConstructorArgs)
                .generateSetterCode(gatheredProperties.values.map { it.propertySpec })

            typeBuilder.addFunction(builder.build())
        }

        /**
         * Extension to create instance from any instance inheriting model interface
         */
        val modelConstructorBuilder = funModelConstructorBuilder(
            impl,
            typeBuilder.propertySpecs,
            defaultConstructorProperties
        )
        typeBuilder
            .addFunction(
                modelConstructorBuilder
                    .build()
            )

        if (impl.implementComparable)
            implementComparable(impl, typeBuilder, impl.modelClassName)

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

        for (dtoImpl in ProcessingContext.listModelDTOs(impl.modelClassName)) {
            funUpdateByBuilder(dtoImpl, typeBuilder, impl.implClassName)
        }

        fileBuilder.addType(typeBuilder.build())


        /**
         * Extension to create DTO instance from any class inheriting model interface
         */
        fileBuilder.addFunction(
            funModelExtensionAsBuilder(impl)
                .build()
        )
        fileBuilder.addFunction(
            funModelIterableExtensionBuilder(impl, "as")
                .build()
        )

        fileSpecs.add(fileBuilder.build())

    }

    private fun funModelExtensionAsBuilder(
        settings: ImmutableSettings
    ): FunSpec.Builder {
        val extToBuilder = FunSpec.builder("as${settings.implClassName.simpleName}")
            .receiver(settings.modelClassName)
        val funBodyBuilder = CodeBlock.builder()

        funBodyBuilder.addStatement("return ${settings.implClassName.simpleName}(this)")


        extToBuilder.addCode(funBodyBuilder.build())

        return extToBuilder
    }

    /**
     * Updates this class by data from other class
     */
    private fun funUpdateByBuilder(
        settings: AbstractSettings<*>,
        typeBuilder: TypeSpec.Builder,
        returns: ClassName
    ) {
        val modelPropertySpecs = settings.modelClassName.asTypeSpec().propertySpecs

        val propertySpecs = settings.typeSpec.propertySpecs
        val param = settings.implClassName.simpleName.replaceFirstChar { it.lowercase() }
        val extToBuilder = FunSpec.builder("updateBy")
            .addParameter(param, settings.implClassName)
            .returns(returns)

        val parentPropNames = typeBuilder.propertySpecs.map { it.name }

        val funBodyBuilder = CodeBlock.builder()
        for (prop in propertySpecs) {
            if (prop.name !in parentPropNames)
                continue

            val nullSafeCall = if(prop.type.isNullable) "?" else ""

            val nullable = prop.type.isNullable
//                    &&
//                    !typeBuilder.propertySpecs.first { it.name == prop.name }.type.isNullable

            val notNullSafe = if (nullable) "!!" else ""

            val modelProp = modelPropertySpecs.firstOrNull { it.name == prop.name }

            val setValue = if (modelProp != null &&
                modelProp.type != firstImplementation(modelProp.type, settings)
            ) {
                val type = firstImplementationImmutable(modelProp.type)
                if (type is ParameterizedTypeName) {

                    if (type.rawType.implements("Iterable")) {
                        val generic = type.typeArguments.first()
                        "this.${prop.name} = " +
                                "$param.${prop.name}${notNullSafe}.map { " +
                                "${generic.asClassName().simpleName}()$nullSafeCall.updateBy(it) " +
                                "}"
                    } else if (type.rawType.implements(Map::class.asTypeName())) {
                        val keyGeneric = type.typeArguments.first()
                        val valueGeneric = type.typeArguments.last()

                        val implementedTypes = ProcessingContext.implementations.map { it.implClassName }

                        val keyMapper =
                            if (keyGeneric in implementedTypes) {
                                ".mapKeys { ${keyGeneric.asClassName().simpleName}()$nullSafeCall.updateBy(it.key) }"
                            } else {
                                ""
                            }
                        val valueMapper =
                            if (valueGeneric in implementedTypes) {
                                ".mapValues { ${valueGeneric.asClassName().simpleName}()$nullSafeCall.updateBy(it.value) }"
                            } else {
                                ""
                            }

                        "this.${prop.name} = " +
                                "$param.${prop.name}${notNullSafe}$keyMapper$valueMapper"
                    } else
                        "this.${prop.name} = $param.${prop.name}${notNullSafe}"
                } else {
                    "this.${prop.name}$nullSafeCall.updateBy($param.${prop.name}$notNullSafe)"
                }
            } else {
                "this.${prop.name} = $param.${prop.name}$notNullSafe"
            }

            if (nullable) {
                funBodyBuilder.beginControlFlow("if($param.${prop.name} != null)")
                funBodyBuilder.add("$setValue\n")
                funBodyBuilder.endControlFlow()
            } else
                funBodyBuilder.add("$setValue\n")
        }
        funBodyBuilder.add("return this")

        extToBuilder.addCode(funBodyBuilder.build())
        typeBuilder.addFunction(extToBuilder.build())


    }
}
