package net.eraga.tools.model

import com.squareup.kotlinpoet.*
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
        val fileBuilder = FileSpec.builder(
                className.packageName,
                className.simpleName)

        val typeBuilder = TypeSpec
                .classBuilder(className)
                .addModifiers(impl.classModifier)

        val kmClass = element.getAnnotation(Metadata::class.java)!!.toImmutableKmClass()
        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)

        kmClassSpec.superinterfaces.keys.first().asTypeSpec().superinterfaces


        var superinterfaces = mutableListOf<TypeName>(impl.modelClassName)


        /**
         * Gather properties and create constructor
         */
        val constructorBuilder = FunSpec.constructorBuilder()

        var propertyNum = 0
        val gatheredProperties = gatherProperties(element, impl.implClassName)

        val skippedProperties = gatheredProperties.filter { it.value.preventOverride }

        if (skippedProperties.isNotEmpty()) {
            superinterfaces = correctInheritanceChainFor(skippedProperties, superinterfaces)
        }

        superinterfaces.forEach {
            typeBuilder.addSuperinterface(it)
        }

        for ((_, propertyData) in gatheredProperties) {
            if (propertyData.preventOverride)
                continue

            val name = propertyData.propertySpec.name

            val defaultInit = propertyData.defaultInit

            val type = determinePropertyType(element, propertyData)

            val kotlinProperty = PropertySpec.builder(name, type)
                    .mutable(true)

            if (propertyData.propertySpec.setter == null && !propertyData.propertySpec.mutable) {
                kotlinProperty
                        .setter(FunSpec.setterBuilder()
                                .addModifiers(KModifier.PRIVATE)
                                .build()
                        )
            }

            if (superinterfaces.any {
                        supersHaveThisProp(it.asTypeSpec(), propertyData.propertySpec)
                    })
                kotlinProperty.addModifiers(KModifier.OVERRIDE)


            val defaultValue = defaultInit ?: constructorDefaultInitializer(
                    impl,
                    type,
                    propertyData)

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
        val modelConstructorBuilder = funModelConstructorBuilder(
                impl, typeBuilder.propertySpecs
        )
        typeBuilder
                .addFunction(modelConstructorBuilder
                        .callThisConstructor()
                        .build())

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


        for (dtoImpl in ProcessingContext.listElementDTOs(impl.modelClassName)) {
            funUpdateByBuilder(dtoImpl, typeBuilder)
        }

        fileBuilder.addType(typeBuilder.build())

        fileSpecs.add(fileBuilder.build())
    }

    /**
     * Updates this class by data from other class
     */
    private fun funUpdateByBuilder(
            settings: AbstractSettings<*>,
            typeBuilder: TypeSpec.Builder
    ) {
        val propertySpecs = settings.typeSpec.propertySpecs
        val param = settings.implClassName.simpleName.replaceFirstChar { it.lowercase() }
        val extToBuilder = FunSpec.builder("updateBy")
                .addParameter(param, settings.implClassName)

        val funBodyBuilder = CodeBlock.builder()
        for (prop in propertySpecs) {
            if (prop.type.isNullable &&
                    !typeBuilder.propertySpecs.first { it.name == prop.name }.type.isNullable) {
                funBodyBuilder.beginControlFlow("if($param.${prop.name} != null)")
                funBodyBuilder.add("this.${prop.name} = $param.${prop.name}!!\n")
                funBodyBuilder.endControlFlow()
            } else
                funBodyBuilder.add("this.${prop.name} = $param.${prop.name}\n")
        }

        extToBuilder.addCode(funBodyBuilder.build())
        typeBuilder.addFunction(extToBuilder.build())
    }
}
