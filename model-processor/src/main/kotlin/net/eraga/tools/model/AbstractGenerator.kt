package net.eraga.tools.model

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.models.*
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeKind
import kotlin.NoSuchElementException

/**
 * **AbstractGenerator**
 *
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
abstract class AbstractGenerator(
        protected val element: TypeElement,
        allElements: List<String>,
        protected var kotlinGenerated: String
) : Runnable {
    protected val elements: List<String> = allElements

    protected abstract fun generate()

    override fun run() {
        generate()
    }

    fun gatherProperties(element: TypeElement, level: Int = 0): Map<String, PropertyData> {
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
            kmClass.properties.first{ it.name == name }
        }
        val findPropName = { ele: ExecutableElement ->
            propNames.first {
                val results = arrayOf("get", "set", "")
                ele.simpleName.toString().lowercase().replaceFirst(it.lowercase(), "") in results
            }
        }



        element.enclosedElements
                .filterIsInstance<ExecutableElement>()
                .filter { onlyGetter(it, findPropName(it)) }
//                .associateBy(findPropName)
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

//        kmClass.properties.forEach { property ->
//            val getter = element.enclosedElements
//                    .filterIsInstance<ExecutableElement>().first {
//                        it.returnType !is NoType && it.simpleName.toString()
//                                .lowercase(Locale.getDefault())
//                                .endsWith(property.name.lowercase())
//                    }
//
//            getters[property.name] = PropertyData(
//                    getter,
//                    property,
//                    propertyInitMap?.getOrDefault(getter.simpleName.toString(), null),
//                    preventOverridesMap?.getOrDefault(getter.simpleName.toString(), null) ?: false,
//                    level > 0
//            )
//        }




        getters.forEach { (k, v) -> println("$level: $k") }
        return getters
    }

    fun funCompareToBuilder(
            propertySpecs: MutableList<PropertySpec>,
            comparableSettings: ImplementComparable
    ): CodeBlock.Builder {
        val orderedProperties = mutableListOf<PropertySpec>()

        if (comparableSettings.order.isNotEmpty()) {
            comparableSettings.order.forEach { name ->
                try {
                    orderedProperties.add(propertySpecs.first { it.name == name })
                } catch (e: NoSuchElementException) {
                    throw NoSuchElementException("Property with name='$name' not found")
                }
            }

            if (comparableSettings.compareAllProperties && orderedProperties.size != propertySpecs.size) {
                propertySpecs.forEach {
                    if (it.name !in comparableSettings.order)
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
                    if (!prop.type.isNullable)
                        funBodyBuilder.add("${prop.name} != other.${prop.name} -> ${prop.name}.compareTo(other.${prop.name})\n")
                    else
                        funBodyBuilder.add("${prop.name} != other.${prop.name} -> compareValues(${prop.name}, other.${prop.name})\n")
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
            hashCodeSettings: ImplementHashCode
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
            equalsSettings: ImplementEquals
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
}
