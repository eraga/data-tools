package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.models.*
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import kotlin.NoSuchElementException

/**
 * **AbstractGenerator**
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:20
 */
@KotlinPoetMetadataPreview
abstract class AbstractGenerator<T : AbstractSettings<*>>(
    val listOfImplementations: List<T>
) : Runnable {
    val IGNORED_ANNOTATIONS = listOf(
        "java.lang.Override"
    )

    val SUPPRESS_SKIP_ME = AnnotationSpec
        .builder(ClassName.bestGuess("kotlin.Suppress"))
        .addMember("\"UNUSED_PARAMETER\"")
        .build()


    protected val fileSpecs: MutableList<FileSpec> = mutableListOf()

    val generatedSpecs: List<FileSpec>
        get() = fileSpecs

    protected abstract fun generate()

    override fun run() {
        generate()
    }

    var classNameSpec: ImmutableKmClass? = null
        protected set

    fun gatherProperties(
        element: TypeElement,
        implClassName: ClassName,
        level: Int = 0
    ): Map<String, PropertyData> {
        val getters = LinkedHashMap<String, PropertyData>()

        for (iface in element.interfaces) {
            val realElement = ProcessingContext.types.asElement(iface)

            if (realElement is TypeElement) {
                getters.putAll(gatherProperties(realElement, implClassName, level + 1))
            }
        }

        val metadata = element.getAnnotation(Metadata::class.java)
            ?: // println("NOTICE: Skipping ${element.qualifiedName} as it has no Kotlin Metadata")
            return getters

        val kmClass = metadata.toImmutableKmClass()
        val typeSpec = kmClass.toTypeSpec(
            ProcessingContext.classInspector
        )

        typeSpec.propertySpecs.forEach { propertySpec ->
            val annotations = propertySpec.annotations //+ (propertySpec.getter?.annotations ?: emptySet())

            val propName = propertySpec.name
//            val defaultInit = annotations.getValueOrNull(Implement.Init::class, "with") as String?

            val simpleName = implClassName.simpleName

            /**
             * Initializers
             */
            val implementInits = annotations.of(Implement.Init::class)
            val defaultInit = implementInits.singleHaving("in", simpleName)?.valueOf("with") as String?
                ?: implementInits.singleHaving("in", "")?.valueOf("with") as String?

            /**
             * Omitting prop implementation
             */
            val skip = annotations.hasValueOf(Implement.Omit::class, "", "in") ||
                    annotations.hasValueOf(Implement.Omit::class, simpleName, "in")

            /**
             * Annotations specific to implementation
             */
            val implementAnnotate = annotations.of(Implement.Annotate::class).allHaving("in", simpleName)

            @Suppress("UNCHECKED_CAST")
            val additionalAnnotations = implementAnnotate.map { annotationSpec ->
                annotationBuilderFromAnnotate(annotationSpec).build()
            }

//            if(additionalAnnotations.isNotEmpty()) {
//                println(additionalAnnotations)
//            }

//            if (implClassName.simpleName.contains("ImmutablePerson") &&
//                    propName == "id" &&
//                    level == 0) {
//                println("At element: ${element.simpleName}:")
//                annotations.hasValueOf(Implement.Omit::class, "", "in")
//                println("   $implClassName:$propName !!!!PASS!!!!: $skip")
//                println("   prop: ${annotations.map { it.typeName }}")
//                println("   gett: ${propertySpec.getter?.annotations?.map { it.typeName }}")
//            }

            getters[propName] = PropertyData(
                defaultInit = defaultInit,
                preventOverride = skip,
                isInherited = level > 0,
                propertySpec = typeSpec.propertySpecs.first { it.name == propName },
                additionalAnnotations = additionalAnnotations
            )
        }
        return getters
    }

    fun determinePropertyType(
        element: TypeElement,
        propertyData: PropertyData,
        generator: AbstractGenerator<*>
    ): TypeName {
        return if (propertyData.propertySpec.type.toString() == "error.NonExistentClass") {

//                propertyData.typeSpec.syntheticMethodForAnnotations
//                val generatedClass = getter.getAnnotation(GeneratedClass::class.java)

            val isConstructorInitializer = { mirror: AnnotationMirror ->
                mirror.annotationType.asElement().simpleName.toString() == GeneratedClass::class.java.simpleName
            }
            val annotatedProps: List<Element>? = element
                .enclosedElements
                .first {
                    it.kind == ElementKind.CLASS && it.simpleName.toString() == "DefaultImpls"
                }
                .enclosedElements
                ?.filter {
                    it.simpleName.contains("annotations") &&
                            it.annotationMirrors.any { mirror ->
                                isConstructorInitializer(mirror)
                            }
                }


            val propertyInitMap = annotatedProps?.filter {
                it.annotationMirrors.any { mirror ->
                    isConstructorInitializer(mirror)
                }
            }?.associateBy({
                it.simpleName.split("$").first()
            }, {
                it.annotationMirrors
                    .first { mirror ->
                        isConstructorInitializer(mirror)
                    }
                    .elementValues
                    .values
                    .first()
                    .value
                    .toString()
            })

            if (propertyInitMap!!.values.first().isNotBlank())
                ClassName.bestGuess(propertyInitMap.values.first())
            else
                propertyData.propertySpec.type
        } else {
            try {
                if (generator is DTOGenerator) {
                    ProcessingContext.listImplementationDTOs(propertyData.propertySpec.type)
                        .first()
                        .implClassName
                } else {
                    ProcessingContext.listModelImmutables(propertyData.propertySpec.type)
                        .first()
                        .implClassName
                }
            } catch (e: Exception) {
                try {
                    ProcessingContext.implementedModels
                        .flatMap { it.listOfImplementations }
                        .first { impl ->
                            impl.implClassName == propertyData.propertySpec.type

                        }.implClassName

                } catch (_: NoSuchElementException) {
                    propertyData.propertySpec.type
                }
            }
        }
    }

    fun constructorDefaultInitializer(
        settings: AbstractSettings<*>,
        type: TypeName,
        propertyData: PropertyData
    ): String {
        return if (type.isNullable) {
            "null"
        } else {
            val returnTypeSpec = propertyData.propertySpec.type.asTypeSpec()
            if (returnTypeSpec.isEnum) {
                try {
                    "${returnTypeSpec.name}.${returnTypeSpec.enumConstants.keys.first()}"
                } catch (e: NoSuchElementException) {
                    "$type.values()[0]"
                }
            } else {
                val simpleTypeName = type.toString().split(".").last()

                if (settings.primitiveInitializers.containsKey(simpleTypeName)) {
                    settings.primitiveInitializers[simpleTypeName].toString()
                } else {
                    try {
                        val typeModel = ProcessingContext
                            .implementedModels
                            .flatMap { it.listOfImplementations }
                            .first { impl ->
                                impl.implClassName == type
                            }

                        val meta = typeModel
                        "${meta.implClassName}()"
                    } catch (e: NoSuchElementException) {
                        val classInitializer = type.classToInitializer(settings.classInitializers)
                        if (classInitializer.contains("NonExistentClass")) {
                            println("$classInitializer == $type")
                            type.toString()
                        }
                        classInitializer
                    }
                }
            }
        }
    }

    fun funCompareToBuilder(
        propertySpecs: MutableList<PropertySpec>,
        comparableSettings: CompareTo
    ): CodeBlock.Builder {
        val orderedProperties = mutableListOf<PropertySpec>()
        fun String.withoutMinus(): String {
            return replace("-", "")
        }
        if (comparableSettings.order.isNotEmpty()) {
            comparableSettings.order.forEach { name ->
                try {
                    orderedProperties.add(propertySpecs.first { it.name == name.withoutMinus() })
                } catch (e: NoSuchElementException) {
                    throw NoSuchElementException("Property with name='$name' not found in $propertySpecs ")
                }
            }
            val orderWithoutMinus = comparableSettings.order.map { it.withoutMinus() }

            if (comparableSettings.compareAllProperties && orderedProperties.size != propertySpecs.size) {
                propertySpecs.forEach {
                    if (it.name !in orderWithoutMinus)
                        orderedProperties.add(it)
                }
            }
        } else {
            orderedProperties.addAll(propertySpecs)
        }

        val funBodyBuilder = CodeBlock.builder()
        if (orderedProperties.size > 0) {
            funBodyBuilder.beginControlFlow("return when")
            for (prop in orderedProperties) {
                if (prop.type.copy(nullable = false).asClassName().implements("Comparable")) {
                    val minus = if (comparableSettings.order.contains("-${prop.name}")) "-" else ""
                    if (!prop.type.isNullable)
                        funBodyBuilder.add("${prop.name} != other.${prop.name} -> $minus${prop.name}.compareTo(other.${prop.name})\n")
                    else
                        funBodyBuilder.add("${prop.name} != other.${prop.name} -> ${minus}compareValues(${prop.name}, other.${prop.name})\n")
                } else {
                    funBodyBuilder.add("/*\n")
                    funBodyBuilder.add("${prop.name}: ${prop.type} does not inherit comparable\n")
                    funBodyBuilder.add("*/\n")
                }
            }
            funBodyBuilder.add("else -> 0\n")
            funBodyBuilder.endControlFlow()
            funBodyBuilder.add(".toInt()\n")
        } else {
            funBodyBuilder.add("return 0\n")
        }

        return funBodyBuilder
    }

    fun funHashCodeBuilder(
        propertySpecs: MutableList<PropertySpec>,
        hashCodeSettings: HashCode
    ): CodeBlock.Builder {
        val funBodyBuilder = CodeBlock.builder()
        funBodyBuilder.add("var hashCode = 0\n")
        if (propertySpecs.size > 0) {
            for (prop in propertySpecs) {
                val isArray = prop.type.asClassName().isArray()
                if (isArray && hashCodeSettings.arrayComparing == ArrayComparing.STRUCTURAL)
                    funBodyBuilder.add("hashCode = 31 * hashCode + %L.contentHashCode()\n", prop.name)
                else if (isArray && hashCodeSettings.arrayComparing == ArrayComparing.STRUCTURAL_RECURSIVE)
                    funBodyBuilder.add("hashCode = 31 * hashCode + %L.contentDeepHashCode()\n", prop.name)
                else
                    funBodyBuilder.add("hashCode = 31 * hashCode + %L.hashCode()\n", prop.name)
            }
        }
        funBodyBuilder.add("return hashCode\n")

        return funBodyBuilder
    }

    fun funEqualsBuilder(
        propertySpecs: MutableList<PropertySpec>,
        className: String,
        equalsSettings: Equals
    ): CodeBlock.Builder {
        val funBodyBuilder = CodeBlock.builder()
        funBodyBuilder.add(
            """
            |if (this === other) return true
            |if (other !is ${className}) return false
            |""".trimMargin()
        )
        funBodyBuilder.add("\n")
        if (propertySpecs.size > 0) {
//            val contentDeepEquals = MemberName("kotlin.collections","contentDeepEquals")
            for (prop in propertySpecs) {
                val isArray = prop.type.asClassName().isArray()
                if (isArray && equalsSettings.arrayComparing == ArrayComparing.STRUCTURAL)
                    funBodyBuilder.add("if (!%L.contentEquals(other.%L)) return false\n", prop.name, prop.name)
                else if (isArray && equalsSettings.arrayComparing == ArrayComparing.STRUCTURAL_RECURSIVE)
                    funBodyBuilder.add("if (!%L.contentDeepEquals(other.%L)) return false\n", prop.name, prop.name)
                else
                    funBodyBuilder.add("if (%L != other.%L) return false\n", prop.name, prop.name)
            }
        }
        funBodyBuilder.add("return true\n")

        return funBodyBuilder
    }

    fun funAllArgsConstructorCode(
        propertySpecs: List<PropertySpec>
    ): CodeBlock.Builder {
        val funBodyBuilder = CodeBlock.builder()
        if (propertySpecs.size > 0) {
            for (prop in propertySpecs) {
                funBodyBuilder.add("this.%L = %L\n", prop.name, prop.name)
            }
        }

        return funBodyBuilder
    }

    fun funModelConstructorBuilder(
        settings: AbstractSettings<*>,
        propertySpecs: List<PropertySpec>
    ): FunSpec.Builder {
        val modelPropertySpecs = settings.modelClassName.asTypeSpec().propertySpecs

        val paramName = settings.modelClassName.simpleName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        val implName = settings.implClassName.simpleName
        val constructorBuilder = FunSpec.constructorBuilder()
        val funBodyBuilder = CodeBlock.builder()
        constructorBuilder.addParameter(paramName, settings.modelClassName)
        if (propertySpecs.isNotEmpty()) {
            for (prop in propertySpecs) {
                val modelProp = modelPropertySpecs.firstOrNull { it.name == prop.name }
                if (modelProp != null &&
//                    modelProp.type.asClassName().simpleName == settings.modelClassName.simpleName
                    modelProp.type.asClassName().simpleName != prop.type.asClassName().simpleName
                ) {
                    funBodyBuilder.add(
                        "this.%L = ${prop.type.asClassName().simpleName}($paramName.%L)\n",
                        prop.name,
                        prop.name
                    )
                } else {
                    funBodyBuilder.add("this.%L = $paramName.%L\n", prop.name, prop.name)
                }
            }
        }

        constructorBuilder.addCode(funBodyBuilder.build())

        return constructorBuilder
    }


    fun funModelExtensionToBuilder(
        settings: AbstractSettings<*>,
        dtoPropertySpecs: List<PropertySpec>
    ): FunSpec.Builder {
        val modelPropertySpecs = settings.modelClassName.asTypeSpec().propertySpecsIncludingInherited().values

        val extToBuilder = FunSpec.builder("to${settings.implClassName.simpleName}")
            .receiver(settings.modelClassName)
            .returns(settings.implClassName)
        val funBodyBuilder = CodeBlock.builder()


        val modelPropNames = modelPropertySpecs.map { it.name }
        val propertySpecs = dtoPropertySpecs.filter { it.name in modelPropNames }

        if (propertySpecs.isNotEmpty()) {
            val props = propertySpecs.joinToString {
                val modelProp = modelPropertySpecs.firstOrNull { model -> model.name == it.name }
                if (modelProp != null &&
                    modelProp.type.asClassName().simpleName != it.type.asClassName().simpleName
                ) {
                    val nonNullType = it.type.copy(nullable = false)
                    val dtoSettings = ProcessingContext.implementations
                        .filterIsInstance<DTOSettings>()
                        .firstOrNull { sett -> sett.implClassName == nonNullType }

                    if (dtoSettings != null) {
                        settings.fileBuilder.addImport(dtoSettings.implClassName.packageName, "to${it.type.asClassName().simpleName}")
                    }
                    "${it.name} = this.${it.name}.to${it.type.asClassName().simpleName}()"
                } else {
                    "${it.name} = this.${it.name}"
                }
            }
            funBodyBuilder.addStatement("return ${settings.implClassName.simpleName}(%L)", props)
        }

        extToBuilder.addCode(funBodyBuilder.build())

        return extToBuilder
    }


    fun implementToString(typeBuilder: TypeSpec.Builder) {
        val propertySpecs = typeBuilder.propertySpecs
        val prebuiltType = typeBuilder.build()

        val funBodyBuilder = CodeBlock.builder()
        funBodyBuilder.add("return \"${prebuiltType.name}(\" + ")
        if (propertySpecs.size > 0) {
            funBodyBuilder.add("\n")
            funBodyBuilder.indent()

            funBodyBuilder.add(propertySpecs.joinToString(", \" + \n") {
                val isString = it.type == String::class.asTypeName()
                val sq = if (isString) "\\\"" else ""
                val toStr = if (isString) "" else ".toString()"
                "\"${it.name} = $sq\${${it.name}$toStr}$sq"
            })

            funBodyBuilder.add("\" + \n")
            funBodyBuilder.unindent()
        }
        funBodyBuilder.add("\")\"")

        val funBody = funBodyBuilder.build()

        val compareToFun = FunSpec.builder("toString")
            .returns(String::class)
            .addModifiers(KModifier.OVERRIDE)
            .addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }

    fun implementEquals(
        settings: AbstractSettings<*>,
        typeBuilder: TypeSpec.Builder
    ) {
        val funBodyBuilder = funEqualsBuilder(
            typeBuilder.propertySpecs,
            typeBuilder.build().name!!,
            settings.equalsSettings
        )
        val funBody = funBodyBuilder.build()

        val compareToFun = FunSpec.builder("equals")
            .returns(Boolean::class)
            .addParameter(
                "other",
                Any::class.asTypeName().copy(nullable = true)
            )
            .addModifiers(KModifier.OVERRIDE)
            .addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }

    fun implementHashCode(
        settings: AbstractSettings<*>,
        typeBuilder: TypeSpec.Builder
    ) {
        val funBodyBuilder = funHashCodeBuilder(
            typeBuilder.propertySpecs,
            settings.hashCodeSettings
        )
        val funBody = funBodyBuilder.build()

        val compareToFun = FunSpec.builder("hashCode")
            .returns(Int::class)
            .addModifiers(KModifier.OVERRIDE)
            .addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }

    fun implementCloneable(
        settings: AbstractSettings<*>,
        typeBuilder: TypeSpec.Builder
    ) {
        val funBodyBuilder = CodeBlock.builder()
            .add("return super.clone() as ${typeBuilder.build().name}")

        val funBody = funBodyBuilder.build()

        val compareToFun = FunSpec.builder("clone")
            .returns(settings.implClassName)
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PUBLIC)
            .addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }


    fun implementComparable(
        settings: AbstractSettings<*>,
        typeBuilder: TypeSpec.Builder,
        compareTo: ClassName = settings.implClassName
    ) {
        val funBodyBuilder =
            funCompareToBuilder(typeBuilder.propertySpecs, settings.comparableSettings)

        val funBody = funBodyBuilder.build()

        val compareToFun = FunSpec.builder("compareTo")
            .returns(Int::class)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder(
                    "other",
                    compareTo
                ).build()
            )
            .addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }

    fun implementCopiable(
        typeBuilder: TypeSpec.Builder,
        kclass: ClassName
    ) {
        val propertySpecs = typeBuilder.propertySpecs
        val prebuiltType = typeBuilder.build()

        val funBodyBuilder = CodeBlock.builder()

        val compareToFun = FunSpec.builder("copy")
            .returns(kclass)

        funBodyBuilder.add("return ${prebuiltType.name}(")
        if (propertySpecs.size > 0) {
            funBodyBuilder.add("\n")
            funBodyBuilder.indent()
            for (prop in propertySpecs) {
                val clone = if (kclass.implements("kotlin.Cloneable"))
                    ".clone()"
                else
                    ""

                compareToFun.addParameter(
                    ParameterSpec.builder(
                        prop.name,
                        prop.type
                    )
                        .defaultValue("this.${prop.name}$clone")
                        .build()
                )
            }
            funBodyBuilder.add(propertySpecs.joinToString(",\n") { "${it.name} = ${it.name}" })

            funBodyBuilder.add("\n")
            funBodyBuilder.unindent()
        }
        funBodyBuilder.add(")")

        val funBody = funBodyBuilder.build()


        compareToFun.addCode(funBody)

        typeBuilder
            .addFunction(compareToFun.build())
    }

    private fun annotationBuilderFromAnnotate(annotationSpec: AnnotationSpec): AnnotationSpec.Builder {
        val type = annotationSpec.valueOf("with") as DeclaredType

        @Suppress("UNCHECKED_CAST")
        val args = annotationSpec.valueOf("args") as List<AnnotationValue>?

        val annotationClass = ClassName.bestGuess(type.toString())

        val builder = AnnotationSpec.builder(annotationClass)
        args?.forEach { arg ->
            builder.addMember(arg.value as String)
        }
        return builder
    }

    fun implementAnnotates(
        typeBuilder: TypeSpec.Builder,
        kmClassSpec: TypeSpec,
        simpleName: String
    ) {
        val implementAnnotate = kmClassSpec.annotationSpecs
            .of(Implement.Annotate::class)
            .allHaving("in", simpleName)

        @Suppress("UNCHECKED_CAST")
        implementAnnotate.forEach { annotationSpec ->
            typeBuilder.addAnnotation(
                annotationBuilderFromAnnotate(annotationSpec).build()
            )
        }
    }

    fun addAnnotations(propertyData: PropertyData, kotlinProperty: PropertySpec.Builder, impl: AbstractSettings<*>) {
        propertyData.propertySpec.annotations
            .filter {
                impl.implementAnnotations.toRegex().matches(it.typeName.toString())
            }
            .forEach {
                if (it.typeName.asClassName().canonicalName !in IGNORED_ANNOTATIONS)
                    kotlinProperty.addAnnotation(it)
            }

        propertyData.additionalAnnotations.forEach {
            kotlinProperty.addAnnotation(it)
        }
    }

    fun supersHaveThisProp(spec: TypeSpec, propertySpec: PropertySpec): Boolean {
        if (spec.propertySpecs.any { it.name == propertySpec.name })
            return true

        return spec.superinterfaces.any { supersHaveThisProp(it.key.asTypeSpec(), propertySpec) }
    }

    private fun supersWithoutTheseProps(type: TypeName, skippedPropSpecs: List<PropertySpec>): MutableList<TypeName> {
        val spec = type.asTypeSpec()
        var possibleSupers = mutableListOf<TypeName>()
        val needsReplace = skippedPropSpecs.any { supersHaveThisProp(spec, it) }

        if (needsReplace) {
            spec.superinterfaces.keys.forEach {
                possibleSupers.addAll(supersWithoutTheseProps(it, skippedPropSpecs))
            }
        } else {
            possibleSupers = mutableListOf(type)
        }
        return possibleSupers
    }

    /**
     * Do not inherit from [superinterfaces] that have declared any of [skippedProperties]
     */
    fun correctInheritanceChainFor(
        skippedProperties: Map<String, PropertyData>,
        superinterfaces: MutableList<TypeName>
    ): MutableList<TypeName> {

        val skippedPropSpecs = skippedProperties.values.map { it.propertySpec }

        val replaceWith: MutableMap<TypeName, MutableList<TypeName>> = mutableMapOf()

        superinterfaces.forEach { typeName ->
            val spec = typeName.asTypeSpec()
            val needsReplace = skippedPropSpecs.any { supersHaveThisProp(spec, it) }

            if (needsReplace) {
                replaceWith[typeName] = supersWithoutTheseProps(typeName, skippedPropSpecs)
            }
        }

        if (replaceWith.isEmpty()) {
            return superinterfaces
        }

        val correctedSupers = mutableListOf<TypeName>()

        superinterfaces.forEach { superType ->
            if (superType in replaceWith.keys)
                correctedSupers.addAll(replaceWith[superType]!!)
            else
                correctedSupers.add(superType)

        }

        return correctedSupers

    }
}
