package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.classFor
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.KmProperty
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor7
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * **kotplinpoet**
 *
 * kotplinpoet extensions
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:25
 */


@KotlinPoetMetadataPreview
fun TypeSpec.propertySpecsIncludingInherited(): Map<String, PropertySpec> {
    val map = mutableMapOf<String, PropertySpec>()
    this.superinterfaces.map { it.key.asTypeSpec() }.forEach {
        map.putAll(it.propertySpecsIncludingInherited())
    }

    this.propertySpecs.forEach {
        map[it.name] = it
    }

    return map
}

/**
 * Annotation value visitor adding members to the given builder instance.
 */
private class AnnotationSpecVisitor(
    val builder: CodeBlock.Builder
) : SimpleAnnotationValueVisitor7<CodeBlock.Builder, String>(builder) {

    override fun defaultAction(o: Any, name: String) =
        builder.add(memberForValue(o))

    override fun visitAnnotation(a: AnnotationMirror, name: String) =
        builder.add("%L", AnnotationSpec.get(a))

    override fun visitEnumConstant(c: VariableElement, name: String) =
        builder.add("%T.%L", c.asType(), c.simpleName)

    override fun visitType(t: TypeMirror, name: String) =
        builder.add("%T::class", t)

    override fun visitArray(values: List<AnnotationValue>, name: String): CodeBlock.Builder {
        builder.add("[⇥⇥")
        values.forEachIndexed { index, value ->
            if (index > 0) builder.add(", ")
            value.accept(this, name)
        }
        builder.add("⇤⇤]")
        return builder
    }

    fun memberForValue(value: Any) = when (value) {
        is Class<*> -> CodeBlock.of("%T::class", value)
        is Enum<*> -> CodeBlock.of("%T.%L", value.javaClass, value.name)
        is String -> CodeBlock.of("%S", value)
        is Float -> CodeBlock.of("%Lf", value)
        is Char -> CodeBlock.of("'%L'", characterLiteralWithoutSingleQuotes(value))
        else -> CodeBlock.of("%L", value)
    }

    fun characterLiteralWithoutSingleQuotes(c: Char): String = when {
        c == '\b' -> "\\b" // \u0008: backspace (BS)
        c == '\t' -> "\\t" // \u0009: horizontal tab (HT)
        c == '\n' -> "\\n" // \u000a: linefeed (LF)
        c == '\r' -> "\\r" // \u000d: carriage return (CR)
        c == '\"' -> "\"" // \u0022: double quote (")
        c == '\'' -> "\\'" // \u0027: single quote (')
        c == '\\' -> "\\\\" // \u005c: backslash (\)
        c.isIsoControl -> String.format("\\u%04x", c.code)
        else -> c.toString()
    }

    val Char.isIsoControl: Boolean
        get() {
            return this in '\u0000'..'\u001F' || this in '\u007F'..'\u009F'
        }
}

class AnnotationSpecMap(
    val map: Map<String, Any>
)

fun AnnotationSpec.valueOf(name: String): Any? {
    return tag(AnnotationSpecMap::class)?.map?.get(name)
}

fun List<AnnotationSpec>.allHaving(name: String, value: Any): List<AnnotationSpec> {
    return filter { it.valueOf(name) == value }
}

fun List<AnnotationSpec>.singleHaving(name: String, value: Any): AnnotationSpec? {
    return singleOrNull { it.valueOf(name) == value }
}


@KotlinPoetMetadataPreview
fun AnnotationMirror.asAnnotationSpec(): List<AnnotationSpec> {
    val className = ClassName.bestGuess(annotationType.toString())

    val kclass = Class.forName(className.reflectionName()).kotlin
    val nonOptionalArgs = kclass.constructors.first().parameters.filter { !it.isOptional }.size

    val instance = try {
        if (kclass == Metadata::class)
            null
        else if (nonOptionalArgs == 0 && kclass.constructors.any { it.parameters.all { p -> p.isOptional } })
            kclass.createInstance()
        else if (nonOptionalArgs == 1 &&
            kclass.memberProperties.first().returnType.classifier == String::class
        ) {
            val initMap = kclass.constructors
                .first().parameters
                .filter { !it.isOptional }
                .associateBy({ it }, {
                    (it.type.classifier as KClass<*>).createInstance()
                }
                )

            kclass.constructors.first().callBy(initMap)
        } else null
    } catch (e: NoSuchMethodException) {
        println("ERROR: NoSuchMethodException for ${e.message} while processing annotation $kclass")
        null
    }

    val result = mutableListOf<AnnotationSpec>()
    /**
     * Extract repeatable annotations from containers
     */
    if (kclass.memberProperties.size == 1 &&
        nonOptionalArgs == 1
    ) {
        val type = kclass.memberProperties.first().returnType.asTypeName()
        if (type is ParameterizedTypeName &&
            type.typeArguments.size == 1 &&
            type.typeArguments.first() is WildcardTypeName
        ) {
            val typeName = (type.typeArguments.first() as WildcardTypeName).outTypes.first()
            val spec = typeName.asTypeSpec()
            if (spec.isAnnotation && spec.annotationSpecs.any { it.typeName.toString() == "kotlin.`annotation`.Repeatable" }) {
                val executableElement = elementValues.keys.first { it.simpleName.toString() == "value" }
                val value = elementValues[executableElement]!!.value
                if (value is List<*>) {
                    value.forEach {
                        if (it is AnnotationMirror)
                            result.addAll(it.asAnnotationSpec())
                    }
                }
                return result
            }
        }
    }

    val map: MutableMap<String, Any> = mutableMapOf()

    val builder = AnnotationSpec.builder(className)
        .tag(this)
        .tag(AnnotationSpecMap(map))

    kclass.memberProperties.forEach {
        val member = CodeBlock.builder()
        val name = it.name
        val key = this.elementValues.keys.firstOrNull { element -> element.simpleName.toString() == it.name }
        if (key != null) {
            member.add("%L = ", name)
            val visitor = AnnotationSpecVisitor(member)
            val value = elementValues[key]!!
            value.accept(visitor, name)
            map[name] = value.value
        } else {
            // Try to fill annotation with default values if there was no-arg constructor
//            val visitor = KmProperty()
//            kmClass.properties.first { prop->prop.name == it.name }.accept(visitor)
            if (instance != null) {
                member.add("%L = ", name)
                val value: Any = it.getter.call(instance)!!
                member.add("%L", value)
                map[name] = value
            }
        }
        builder.addMember(member.build())
    }

    return result.apply { add(builder.build()) }
}

fun List<AnnotationSpec>.of(kclass: KClass<out Annotation>): List<AnnotationSpec> {
    return filter {
        it.typeName == kclass.asTypeName()
    }
}

/**
 * Has one or more of such annotations
 */
fun List<AnnotationSpec>.has(
    kclass: KClass<out Annotation>
): Boolean {
    return of(kclass).isNotEmpty()
}


fun List<AnnotationSpec>.getValueOrNull(
    kclass: KClass<out Annotation>,
    name: String = "value"
): Any? {
    val annotation = of(kclass).singleOrNull() ?: return null
    val values = annotation.tag(AnnotationSpecMap::class)!!.map
    val key = values.keys.firstOrNull { it == name } ?: return null

    return values[key]!!
}

fun List<AnnotationSpec>.hasValueOf(
    kclass: KClass<out Annotation>,
    value: Any,
    name: String = "value"
): Boolean {
    if (isEmpty())
        return false

//    val defaultGetter = kclass.memberProperties.firstOrNull { it.name == name }?.getter
//            ?: throw IllegalArgumentException("$kclass has no argument with name '$name'")

    /**
     * Check if we have any annotations with this value
     */
    val first = of(kclass).firstOrNull { annotation ->
        val values = annotation.tag(AnnotationSpecMap::class)!!.map
        val key = values.keys.firstOrNull { it == name }
            ?: return@firstOrNull false

        values[key]!! == value
    }
    if (first != null)
        return true

//    /**
//     * Didn't find value, lets get default value by reflection
//     * and compare it with any instance which has default value
//     */
//    if (has(kclass)) {
//        val defaultInstance = kclass.createInstance()
//        val defaultValue = defaultGetter.call(defaultInstance)
//
//        return defaultValue == value
//    }
//
//    /**
//     * Didn't find instance, check if we have Repeatable
//     */
//    val regex = "@java.lang.annotation.Repeatable\\(value=interface (net.eraga.*)\\)".toRegex()
//
//    val groupKClass = kclass.annotations.filter {
//        regex.matches(it.toString())
//    }.map {
//        regex.find(it.toString())!!.groupValues[1]
//    }.map {
//        Class.forName(it).kotlin
//    }.singleOrNull() ?: return false
//
//    groupKClass as KClass<out Annotation>
//
//
//    if (has(groupKClass)) {
//        val list = getValueOrNull(groupKClass) as List<AnnotationMirror>?
//        list?.forEach { annotation ->
//            val key = annotation.elementValues.keys
//                    .firstOrNull { it.simpleName.toString() == name }
//
//            if (key != null && annotation.elementValues[key]!!.value == value)
//                return true
//        }
//        // Didn't find value, so we have to get default value by reflection
////        val defaultInstance = groupKClass.createInstance()
////        val defaultValue = groupKClass.memberProperties
////                .firstOrNull { it.name == name }
////                ?.getter
////                ?.call(defaultInstance) ?: return false
////
////        return defaultValue == value
//    }

    return false
}


@OptIn(DelicateKotlinPoetApi::class)
@KotlinPoetMetadataPreview
fun TypeMirror.asClassName(): TypeName {
    return try {
        val kmClass = toImmutableKmClass()!!
        val parameters = kmClass.typeParameters
        ClassName(kmClass.moduleName!!, kmClass.name).apply {
            if (parameters.isNotEmpty())
                parameterizedBy(parameters.map { ClassName("", it.name) })
        }
    } catch (e: Exception) {
        asTypeName()
    }
}

@KotlinPoetMetadataPreview
fun TypeMirror.toImmutableKmClass(): ImmutableKmClass? {
    return getAnnotation(Metadata::class.java)?.toImmutableKmClass()
}

//@KotlinPoetMetadataPreview
//fun TypeName.asClassNameTypeSpec(): ClassNameSpec {
//    return ClassNameSpec(asClassName(), asTypeSpec())
//}

fun TypeName.asClassName(): ClassName {
    if (this is ClassName) {
        return this
    } else if (this is ParameterizedTypeName) {
        return this.rawType
    }

    return ClassName("", toString())
//    throw IllegalArgumentException("$this is not ClassName")
}

fun ClassName.isArray(): Boolean {
    // TODO: fix it
    if (canonicalName.contains("Array")) {
//        println(canonicalName)
        return true
    }
    return false
}

@KotlinPoetMetadataPreview
fun ClassName.implementsInJava(name: String): Boolean {
    try {
        if (name.contains("Comparable") and isPrimitiveComparable())
            return true
    } catch (e: IllegalArgumentException) {
    }

    val type = if (isKotlinIntrinsic()) {
        val javaName = kotlinIntrinsicToJava()
        if (javaName != null)
            ProcessingContext.elements.getTypeElement(javaName)?.asType()
        else
            null
    } else {
        ProcessingContext.elements.getTypeElement(canonicalName)?.asType()
    }

    if (type != null)
        return ProcessingContext.types.allSupertypes(type).any {
            it.toString().contains(name)
        }

    return false
}

@KotlinPoetMetadataPreview
fun ImmutableKmClass.asClassName(): ClassName {
    return ClassInspectorUtil.createClassName(name)
}

@KotlinPoetMetadataPreview
fun ImmutableKmClass.asTypeSpec(): TypeSpec {
    return toTypeSpec(ProcessingContext.classInspector)
}


@KotlinPoetMetadataPreview
fun TypeElement.implements(kclass: KClass<*>): Boolean {
    val kmClass = this.toImmutableKmClass()

    return kmClass.supertypes.any { it.classifier.classString() == kclass.qualifiedName } ||
            this.asClassName().implements(kclass.simpleName.toString())
}

@KotlinPoetMetadataPreview
fun ClassName.implements(name: String): Boolean {
    if (this in KOTLIN_ARRAY_INTERFACES)
        return false

    if (implementsInJava(name))
        return true

    val elementsClassInspector = ProcessingContext.classInspector

    if (ClassName.bestGuess(name).canonicalName == canonicalName)
        return true

    val km = when (val className = bestGuessParametrized()) {
        is ClassName -> {

            if (className.implementsInJava(name))
                return true

            if (!className.isKotlinIntrinsic())
                try {
                    elementsClassInspector.classFor(className)
                } catch (e: IllegalStateException) {
                    null
                } catch (e: NullPointerException) {
                    null
                }
            else
                null
        }
        is ParameterizedTypeName -> {
            if (className.rawType.implementsInJava(name))
                return true
            if (!className.rawType.isKotlinIntrinsic())
                elementsClassInspector.classFor(className.rawType)
            else
                null
        }
        else -> {
            throw IllegalArgumentException("$className must be of instance ClassName or ParameterizedTypeName")
        }
    }
    return km?.supertypes?.any { it.classifier.classString().contains(name) } == true
}

val KOTLIN_PRIMITIVE_TYPES = setOf(
    UNIT,
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    CHAR,
    FLOAT,
    DOUBLE,
    STRING
)


fun TypeName.typeArguments(): List<TypeName> {
    if (this is ParameterizedTypeName) {
        return typeArguments
    }
    return emptyList()
}
