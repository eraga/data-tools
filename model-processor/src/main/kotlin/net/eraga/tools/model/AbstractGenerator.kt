package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.AnnotationSpec.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.model.ProcessingContext.firstImplementation
import net.eraga.tools.model.ProcessingContext.firstImplementationDTO
import net.eraga.tools.models.*
import net.eraga.tools.models.Implement.AnnotationSetting.Target
import net.eraga.tools.models.Implement.AnnotationSetting.Target.*
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import kotlin.NoSuchElementException
import kotlin.reflect.KClass

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
        spec: FileSpec,
        implClassName: ClassName,
        level: Int = 0
    ): Map<String, PropertyData> {
        val getters = LinkedHashMap<String, PropertyData>()
        val typeSpec = spec.singleTypeSpec()
        val supers = mutableListOf<TypeName>()

        if (typeSpec.superclass != Any::class.asTypeName())
            supers.add(typeSpec.superclass)

        supers.addAll(typeSpec.superinterfaces.keys)

        for (iface in supers) {
            val ifaceFileSpec = iface.asFileSpec()
            val ifaceTypeVars = ifaceFileSpec.singleTypeSpec().typeVariables.map { it.toString() }

            val typeVarsToArgs = mutableMapOf<String, TypeName>()

            if (ifaceTypeVars != iface.typeArguments().map { it.toString() }) {
                ifaceTypeVars.forEachIndexed { index, it ->
                    typeVarsToArgs[it] = iface.typeArguments()[index]
                }
            }

            val ifaceProperties = gatherProperties(
                iface.asFileSpec(),
                implClassName,
                level + 1
            )
            // Replace generic type varaiables

            if (typeVarsToArgs.isNotEmpty())
                ifaceProperties.values.forEach {
                    val type = it.propertySpec.type
                    if (type !is ParameterizedTypeName &&
                        type.toString() in typeVarsToArgs.keys
                    ) {
                        val builder = it.propertySpec.toBuilder(
                            type = typeVarsToArgs[type.toString()]!!
                        )
                        it.propertySpec = builder.build()
                    } else if (
                        type is ParameterizedTypeName
                    ) {
                        val remappedTypeArgs = mutableListOf<TypeName>()
                        type.typeArguments.forEach { typeArgument ->
                            if (typeArgument.toString() in typeVarsToArgs.keys) {
                                remappedTypeArgs.add(
                                    typeVarsToArgs[typeArgument.toString()]!!
                                )
                            } else {
                                remappedTypeArgs.add(typeArgument)
                            }
                        }
                        val builder = it.propertySpec.toBuilder(
                            type = type.rawType.parameterizedBy(remappedTypeArgs)
                        )
                        it.propertySpec = builder.build()
                    }
                }

            getters.putAll(
                ifaceProperties
            )
        }

        typeSpec.propertySpecs.forEach { originalPropertySpec ->
            var propertySpec = originalPropertySpec
            val annotations = propertySpec.annotations //+ (propertySpec.getter?.annotations ?: emptySet())

            val propName = propertySpec.name

            val simpleName = implClassName.simpleName

            val noInits = annotations.of(Implement.NoInit::class)
            val noInit = noInits.singleHaving("in", simpleName)
                ?: noInits.singleHaving("in", "")
            val constructorInit = noInit != null

            /**
             * Annotation settings
             */
            val annotationReplacements = mutableMapOf<AnnotationSpec, AnnotationSpec>()
            val annotationSettings = annotations.of(Implement.AnnotationSetting::class)
            if (annotationSettings.isNotEmpty()) {
                val correctedAnnotations = mutableListOf<AnnotationSpec>()
                annotationSettings.forEach { annotation ->
                    @Suppress("UNCHECKED_CAST")
                    val classes = annotation.arrayValueOf("classes")!!
                        .map { type ->
                            (type.value as DeclaredType).asElement()
                        }
                        .filterIsInstance<TypeElement>()
                        .map {
                            Class.forName(it.qualifiedName.toString()).kotlin as KClass<out Annotation>
                        }

                    val targetElement = annotation.valueOf("target") as VariableElement
                    val target = valueOf(targetElement.simpleName.toString())
//                    println(classes?.first())
                    for (kclass in classes) {
                        println("$kclass to $target")
                        val annosToReplace = annotations.of(kclass)
                        if (annosToReplace.isEmpty())
                            continue
                        for (anno in annosToReplace) {
                            when (target) {
                                NONE -> {
                                    if (anno.useSiteTarget == null)
                                        continue
                                    annotationReplacements[anno] =
                                        anno.toBuilder()
                                            .useSiteTarget(null)
                                            .build()
                                }
                                INHERIT -> {
                                }
                                else -> {
                                    if (anno.useSiteTarget != null &&
                                        anno.useSiteTarget!!.name.lowercase() == target.siteTarget)
                                        continue

                                    annotationReplacements[anno] =
                                        anno.toBuilder()
                                            .useSiteTarget(
                                                UseSiteTarget.valueOf(target.name)
                                            )
                                            .build()
                                }
                            }
                        }
                    }
                }
//                annotations.of()
            }

            if (annotationReplacements.isNotEmpty()) {
                val replacedAnnotations = mutableListOf<AnnotationSpec>()
                annotations.forEach {
                    if (it in annotationReplacements)
                        replacedAnnotations.add(annotationReplacements[it]!!)
                    else
                        replacedAnnotations.add(it)
                }
                val newPropSpec = propertySpec.toBuilder()
                newPropSpec.annotations.clear()
                newPropSpec.annotations.addAll(replacedAnnotations)
                propertySpec = newPropSpec.build()
            }

            /**
             * Initializers
             */
            val defaultInit: String? = if (constructorInit) {
                null
            } else {
                val implementInits = annotations.of(Implement.Init::class)
                implementInits.singleHaving("in", simpleName)?.valueOf("with") as String?
                    ?: implementInits.singleHaving("in", "")?.valueOf("with") as String?
            }

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

            getters[propName] = PropertyData(
                defaultInit = defaultInit,
                constructorInit = constructorInit,
                preventOverride = skip,
                isInherited = level > 0,
                propertySpec = propertySpec,
                additionalAnnotations = additionalAnnotations,
                isFinal = propertySpec.tags.values.filterIsInstance<ImmutableKmProperty>().first().isFinal
            )
        }
        return getters
    }

    fun determinePropertyType(
        element: TypeElement,
        propertyData: PropertyData,
        generator: AbstractGenerator<*>
    ): TypeName {
        if (propertyData.propertySpec.type.toString() == "T") {
            println(propertyData.propertySpec.type.toString())
        }
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
//            try {
            if (generator is DTOGenerator) {
                ProcessingContext.firstImplementationDTO(propertyData.propertySpec.type)
            } else {
                ProcessingContext.firstImplementationImmutable(propertyData.propertySpec.type)
            }
//            } catch (e: Exception) {
//                try {
//                    ProcessingContext.implementedModels
//                        .flatMap { it.listOfImplementations }
//                        .first { impl ->
//                            impl.implClassName == propertyData.propertySpec.type
//
//                        }.implClassName
//
//                } catch (_: NoSuchElementException) {
//                    propertyData.propertySpec.type
//                }
//            }
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

                        "${typeModel.implClassName}()"
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

    fun FunSpec.Builder.generateSetterCode(
        propertySpecs: List<PropertySpec>
    ): FunSpec.Builder {
        val propNames = propertySpecs.map { it.name }
        for (param in parameters) {
            if (param.name !in propNames)
                continue

            addCode("this.${param.name} = ${param.name}\n")
        }
        return this
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
        propertySpecs: List<PropertySpec>,
        defaultConstructorProperties: MutableList<PropertySpec>
    ): FunSpec.Builder {
        val modelPropertySpecs = settings.modelClassName.asTypeSpec().propertySpecs
        val defaultConstructorPropertiesNames = defaultConstructorProperties.map { it.name }

        val paramName = settings.modelClassName.simpleName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
//        val implName = settings.implClassName.simpleName
        val constructorBuilder = FunSpec.constructorBuilder()
        val funBodyBuilder = CodeBlock.builder()
        constructorBuilder.addParameter(paramName, settings.modelClassName)

        val thisCallParams = mutableListOf<String>()

        if (propertySpecs.isNotEmpty()) {
            for (prop in propertySpecs) {
                if (prop.name !in defaultConstructorPropertiesNames) {
                    val modelProp = modelPropertySpecs.firstOrNull { it.name == prop.name }
                    if (modelProp != null &&
                        modelProp.type != firstImplementation(modelProp.type, settings)
                    ) {
                        val type = firstImplementation(modelProp.type, settings)
                        if (type is ParameterizedTypeName) {
                            if (type.rawType.implements(Iterable::class.asTypeName())) {
                                val typeName = registerAndGetExtForTypeName(
                                    modelProp.type,
                                    "as",
                                    settings
                                )

                                funBodyBuilder.add(
                                    "this.%L = $paramName.%L.$typeName()\n",
                                    prop.name,
                                    prop.name
                                )
                            } else if (type.rawType.implements(Map::class.asTypeName())) {
                                val keyGeneric = type.typeArguments.first()
                                val valueGeneric = type.typeArguments.last()

                                val implementedTypes = ProcessingContext.implementations.map { it.implClassName }

                                val keyMapper =
                                    if (keyGeneric in implementedTypes) {
                                        ".mapKeys { ${keyGeneric.asClassName().simpleName}(it.key) }"
                                    } else {
                                        ""
                                    }
                                val valueMapper =
                                    if (valueGeneric in implementedTypes) {
                                        ".mapValues { ${valueGeneric.asClassName().simpleName}(it.value) }"
                                    } else {
                                        ""
                                    }
                                funBodyBuilder.add(
                                    "this.%L = $paramName.%L$keyMapper$valueMapper\n",
                                    prop.name,
                                    prop.name
                                )
                            }
                        } else {
                            val typeName = registerAndGetExtForTypeName(
                                modelProp.type,
                                "as",
                                settings
                            )

                            funBodyBuilder.add(
                                "this.%L = $paramName.%L.$typeName()\n",
                                prop.name,
                                prop.name
                            )
                        }
                    } else {
                        funBodyBuilder.add("this.%L = $paramName.%L\n", prop.name, prop.name)
                    }
                } else {
                    thisCallParams.add(prop.name)
                }
            }
        }

        constructorBuilder.addCode(funBodyBuilder.build())
        constructorBuilder.callThisConstructor(thisCallParams.joinToString(", ") { "$it = $paramName.$it" })

        return constructorBuilder
    }

    fun funModelIterableExtensionBuilder(
        settings: AbstractSettings<*>,
        prefix: String
    ): FunSpec.Builder {

        val extToBuilder = FunSpec.builder("$prefix${settings.implClassName.simpleName}")
            .receiver(
                Iterable::class.asClassName().parameterizedBy(settings.modelClassName)
            )

        val funBodyBuilder = CodeBlock
            .builder()
            .add("return this.map { it.$prefix${settings.implClassName.simpleName}() }")

        extToBuilder.addCode(funBodyBuilder.build())

        return extToBuilder
    }

    fun registerAndGetExtForTypeName(
        incType: TypeName,
        prefix: String,
        incSettings: AbstractSettings<*>
    ): String {
        val nonNullType = incType.copy(nullable = false)
        val type = firstImplementation(nonNullType, incSettings)
        val typeClass: ClassName = if (type is ParameterizedTypeName) {
            type.typeArguments.singleOrNull()?.asClassName() ?: type.asClassName()
        } else {
            type.asClassName()
        }
        val typeName = typeClass.simpleName

        val extTypeName = "$prefix$typeName"

        incSettings.fileBuilder.addImport(
            typeClass.packageName,
            "$prefix$typeName"
        )
        return extTypeName
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
            val props = propertySpecs.joinToString(",\n\t", "\n\t", "\n") {
                val modelProp = modelPropertySpecs.firstOrNull { model -> model.name == it.name }
                if (modelProp != null &&
                    modelProp.type != firstImplementationDTO(modelProp.type)
                ) {
                    val type = firstImplementationDTO(modelProp.type)
                    if (type is ParameterizedTypeName && type.rawType.implements(Map::class.asTypeName())) {
                        val keyGeneric = type.typeArguments.first()
                        val valueGeneric = type.typeArguments.last()

                        val implementedTypes = ProcessingContext.implementations.map { it.implClassName }

                        val keyMapper =
                            if (keyGeneric in implementedTypes) {
                                val typeName: String = registerAndGetExtForTypeName(keyGeneric, "to", settings)
                                ".mapKeys { it.key.$typeName() }"
                            } else {
                                ""
                            }
                        val valueMapper =
                            if (valueGeneric in implementedTypes) {
                                val typeName: String = registerAndGetExtForTypeName(valueGeneric, "to", settings)
                                ".mapValues { it.value.$typeName() }"
                            } else {
                                ""
                            }
                        "${it.name} = this.${it.name}$keyMapper$valueMapper"
                    } else {
                        val typeName: String = registerAndGetExtForTypeName(it.type, "to", settings)

                        "${it.name} = this.${it.name}.$typeName()"
                    }
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

    private fun annotationBuilderFromAnnotate(annotationSpec: AnnotationSpec): Builder {
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
