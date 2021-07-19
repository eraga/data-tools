package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.models.*
import net.eraga.tools.models.CompareTo
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
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
            level: Int = 0): Map<String, PropertyData> {
        val getters = LinkedHashMap<String, PropertyData>()

        for (iface in element.interfaces) {
            val realElement = ProcessingContext.types.asElement(iface)

            if (realElement is TypeElement) {
                getters.putAll(gatherProperties(realElement, level + 1))
            }
        }

        val metadata = element.getAnnotation(Metadata::class.java)
        if (metadata == null) {
            println("NOTICE: Skipping ${element.qualifiedName} as it has no Kotlin Metadata")
            return getters
        }
        val onlyGetter: (ExecutableElement, String) -> Boolean = { ele: ExecutableElement, propName: String ->
            ele.parameters.isEmpty() && ele.returnType.kind != TypeKind.VOID &&
                    ele.simpleName.toString().lowercase().replace(propName.lowercase(), "")
                            .replace("get", "").isBlank()

//            !ele.simpleName.toString().lowercase().replace(propName.lowercase(), "")
//                    .startsWith("set")
        }


        val kmClass = metadata.toImmutableKmClass()
        val typeSpec = kmClass.toTypeSpec(
                ProcessingContext.classInspector
        )

        var propertyInitMap: Map<String, String>? = null
        var preventOverridesMap: Map<String, Boolean>? = null

        val isConstructorInitializer = { mirror: AnnotationMirror ->
            mirror.annotationType.asElement().simpleName.toString() == ConstructorInitializer::class.java.simpleName
        }
        val isPreventOverride = { mirror: AnnotationMirror ->
            mirror.annotationType.asElement().simpleName.toString() == PreventOverride::class.java.simpleName
        }

        val annotationFilterPredicate = { mirror: AnnotationMirror ->
            isPreventOverride(mirror) ||
                    isConstructorInitializer(mirror)
        }

        if (typeSpec.kind == TypeSpec.Kind.INTERFACE) {
            try {
                val annotatedProps: List<Element>? = element
                        .enclosedElements
                        .first {
                            it.kind == ElementKind.CLASS && it.simpleName.toString() == "DefaultImpls"
                        }
                        .enclosedElements
                        ?.filter {
                            it.simpleName.contains("annotations") &&
                                    it.annotationMirrors.any { mirror ->
                                        annotationFilterPredicate(mirror)
                                    }
                        }


                propertyInitMap = annotatedProps?.filter {
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


                preventOverridesMap = annotatedProps?.filter {
                    it.annotationMirrors.any { mirror ->
                        isPreventOverride(mirror)
                    }
                }?.associateBy({
                    it.simpleName.split("$").first()
                }, {
                    true
                })

            } catch (_: NoSuchElementException) {
            }
        }

        val propNames = kmClass.properties.map { it.name }
        val findPropByName = { name: String ->
            try {
                kmClass.properties.first { it.name == name }
            } catch (e: Exception) {
                throw IllegalStateException("Caught Exception when iterating ${kmClass.properties.map { it.name }} " +
                        "and searching '$name' in class ${element.qualifiedName}", e)
            }
        }
        val findPropName = { ele: ExecutableElement ->
            propNames.first {
                val results = arrayOf("get", "set", "")
                ele.simpleName.toString().lowercase().replaceFirst(it.lowercase(), "") in results
            }
        }



        element.enclosedElements
                .filterIsInstance<ExecutableElement>()
//                .associateBy(findPropName)
                .filter {
                    try {
                        findPropName(it)
                        true
                    } catch (e: NoSuchElementException) {
                        false
                    }
                }
                .filter { onlyGetter(it, findPropName(it)) }
                .forEach { getter ->
                    val propName = findPropName(getter)
                    getters[propName] = PropertyData(
                            getter,
                            findPropByName(propName),
                            propertyInitMap?.getOrDefault(getter.simpleName.toString(), null),
                            preventOverridesMap?.getOrDefault(getter.simpleName.toString(), null) ?: false,
                            level > 0,
                            typeSpec.propertySpecs.first { it.name == propName }
                    )
//                    println("$level: ${it.simpleName} (${it.kind})")
                }

//        getters.forEach { (k, _) -> println("$level: $k") }
        return getters
    }

    fun determinePropertyType(
            element: TypeElement,
            propertyData: PropertyData): TypeName {
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

    fun constructorDefaultInitializer(
            settings: AbstractSettings<*>,
            type: TypeName,
            propertyData: PropertyData): String {
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
        val paramName = settings.modelClassName.simpleName
        val constructorBuilder = FunSpec.constructorBuilder()
        val funBodyBuilder = CodeBlock.builder()
        constructorBuilder.addParameter(paramName, settings.modelClassName)
        if (propertySpecs.size > 0) {
            for (prop in propertySpecs) {
                funBodyBuilder.add("this.%L = $paramName.%L\n", prop.name, prop.name)
            }
        }

        constructorBuilder.addCode(funBodyBuilder.build())

        return constructorBuilder
    }


    fun funModelExtensionToBuilder(
            settings: AbstractSettings<*>,
            propertySpecs: List<PropertySpec>
    ): FunSpec.Builder {
        val extToBuilder = FunSpec.builder("to${settings.implClassName.simpleName}")
                .receiver(settings.modelClassName)
                .returns(settings.implClassName)
        val funBodyBuilder = CodeBlock.builder()

        if (propertySpecs.isNotEmpty()) {
            val props = propertySpecs.joinToString { "${it.name} = this.${it.name}" }
            funBodyBuilder.addStatement("return ${settings.implClassName.simpleName}(%L)", props)
        }

        extToBuilder.addCode(funBodyBuilder.build())

        return extToBuilder
    }



    fun implementToString(typeBuilder: TypeSpec.Builder) {
        throw UnsupportedOperationException("not implemented") //TODO
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
            typeBuilder: TypeSpec.Builder) {
        val funBodyBuilder = funHashCodeBuilder(
                typeBuilder.propertySpecs,
                settings.hashCodeSettings)
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
            typeBuilder: TypeSpec.Builder) {
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

//    fun supersHaveThisProp(spec: TypeSpec, propertySpec: PropertySpec): Boolean {
//        return if(spec.superinterfaces.isEmpty())
//            spec.propertySpecs.contains(propertySpec)
//        else
//            spec.superinterfaces.any { supersHaveThisProp(it.key.asTypeSpec(), propertySpec) }
//    }

    fun addAnnotations(propertyData: PropertyData, kotlinProperty: PropertySpec.Builder, impl: AbstractSettings<*>) {
        propertyData.propertySpec.annotations
                .filter {
                    impl.implementAnnotations.toRegex().matches(it.typeName.toString())
                }
                .forEach {
                    if (it.typeName.asClassName().canonicalName !in IGNORED_ANNOTATIONS)
                        kotlinProperty.addAnnotation(it)
                }
    }

    fun supersHaveThisProp(spec: TypeSpec, propertySpec: PropertySpec): Boolean {
        return if(spec.superinterfaces.isEmpty())
            spec.propertySpecs
                    .any{it.name == propertySpec.name}
        else
            spec.superinterfaces.any { supersHaveThisProp(it.key.asTypeSpec(), propertySpec) }
    }
}
