package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isNullable
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.KmClassifier
import net.eraga.tools.models.ClassInitializers
import net.eraga.tools.models.ClassMapping
import net.eraga.tools.models.PrimitiveInitializers
import net.eraga.tools.models.classMappingDefaults
import org.jetbrains.annotations.Nullable
import java.util.concurrent.ConcurrentLinkedQueue
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

fun primitiveInitializersMap(mappings: PrimitiveInitializers?): Map<String, Any?> {
    val defaults = mappings ?: PrimitiveInitializers::class.createInstance()

    val map = HashMap<String, Any?>()

    map["Boolean"] = defaults.Boolean
    map["Byte"] = defaults.Byte
    map["Double"] = defaults.Double
    map["Float"] = defaults.Float
    map["Int"] = defaults.Int
    map["Long"] = defaults.Long
    map["Short"] = defaults.Short
    map["String"] = "\"${defaults.String}\""

    return map
}

fun classInitializersMap(initializers: ClassInitializers?): Map<TypeName, ClassName> {
    val model = initializers ?: ClassInitializers::class.constructors.first().call(arrayOf<ClassMapping>())
    val map = HashMap<TypeName, ClassName>()

//    val processSource = { k: KClass<*> ->
//        try {
//            k.asTypeName()
//        } catch (e: MirroredTypeException) {
//            e.typeMirror.asTypeName().javaToKotlinType()
//        }
//    }
//
//
//    val processIt = { key: KClass<*>, value: KClass<*> ->
//        val source = try {
//            key.asTypeName()
//        } catch (e: MirroredTypeException) {
//            e.typeMirror.asTypeName().javaToKotlinType()
//        }
//
//        val target = try {
//            value.asClassName()
//        } catch (e: MirroredTypeException) {
//            ClassName.bestGuess(e.typeMirror.asTypeName().javaToKotlinType().toString())
//        }
//
//        map[source] =
//            target
//    }
//
//    model.classMappingDefaults().forEach {
//        processIt(it.key, it.value)
//    }
//
//    model.list.forEach {
//        processIt(it.source, it.target)
//    }

    model.classMappingDefaults().forEach {
        val source = try {
            it.key.asTypeName()
        } catch (e: MirroredTypeException) {
            e.typeMirror.asTypeName().javaToKotlinType()
        }

        val target = try {
            it.value.asClassName()
        } catch (e: MirroredTypeException) {
            ClassName.bestGuess(e.typeMirror.asTypeName().javaToKotlinType().toString())
        }

        map[source] =
                target
    }

    model.list.forEach {
        val source = try {
            it.source.asTypeName()
        } catch (e: MirroredTypeException) {
            e.typeMirror.asTypeName().javaToKotlinType()
        }

        val target = try {
            it.target.asClassName()
        } catch (e: MirroredTypeException) {
            ClassName.bestGuess(e.typeMirror.asTypeName().javaToKotlinType().toString())
        }

        map[source] =
                target
    }

    return map
}

//@KotlinPoetMetadataPreview
//fun ClassInspector.kmClassFor(className: ClassName): ImmutableKmClass {
//    if (className in ClassInspectorUtil.KOTLIN_INTRINSIC_INTERFACES) {
//        return Class.forName(
//            JavaToKotlinClassMap.INSTANCE
//            .mapKotlinToJava(FqName(className.toString()).toUnsafe())
//            ?.asSingleFqName()
//            ?.asString()
//        ).toImmutableKmClass()
//    }
//    return classFor(className)
//}

val KOTLIN_ARRAY_INTERFACES: Set<ClassName> = setOf(
        ARRAY,
        BOOLEAN_ARRAY,
        BYTE_ARRAY,
        CHAR_ARRAY,
        SHORT_ARRAY,
        INT_ARRAY,
        LONG_ARRAY,
        FLOAT_ARRAY,
        DOUBLE_ARRAY
)

fun ClassName.Companion.fromKClass(kclass: KClass<*>): ClassName {
    return ClassName.bestGuess(kclass.qualifiedName!!)
}

@KotlinPoetMetadataPreview
fun ClassName.isKotlinIntrinsic(): Boolean {
    return try {
        this in ClassInspectorUtil.KOTLIN_INTRINSIC_INTERFACES
    } catch (e: IllegalArgumentException) {
        println(e.message)
        false
    }
}

fun ClassName.kotlinIntrinsicToJava(): String? {
    return JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(FqName(canonicalName).toUnsafe())
            ?.asSingleFqName()
            ?.asString()
}


@KotlinPoetMetadataPreview
fun ImmutableKmType.toKotlinType(type: TypeName): TypeName {
    val result = if (type is ParameterizedTypeName) {
        ClassName.bestGuess(this.classifier.classString()).parameterizedBy(
                *type.typeArguments.map { it.javaToKotlinType() }.toTypedArray()
        )
    } else {
        ClassName.bestGuess(this.classifier.classString())
    }

    return if (isNullable) result.copy(nullable = true) else result
}

fun TypeName.kotlinToJavaType() {
    JavaToKotlinClassMap.INSTANCE
            .mapKotlinToJava(FqName(asClassName().toString()).toUnsafe())
            ?.asSingleFqName()?.asString()
}


fun TypeName.javaToKotlinType(type: Element? = null): TypeName {
    val annotation = type?.getAnnotation(Nullable::class.java)

    val result = if (this is ParameterizedTypeName) {
        val className =
                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(rawType.toString()))
                        ?.asSingleFqName()?.asString()

        if (className == null) {
            this
        } else {
            (ClassName.bestGuess(className)).parameterizedBy(
                    *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
            )
        }
    } else {
        val className =
                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))
                        ?.asSingleFqName()?.asString()

        if (className == null) {
            this
        } else {
            ClassName.bestGuess(className)
        }
    }
//    println("annotation $annotation of type $result from ${type?.simpleName}")

    return if (annotation != null) result.copy(nullable = true) else result
}

fun TypeName.classToInitializer(map: Map<TypeName, ClassName>): String {
    if (this is ParameterizedTypeName &&
            map.containsKey(rawType)) {
        "${map[rawType]!!}()"
    }

    if (this.toString().startsWith("kotlin.Array"))
        return "arrayOf()"

    if (this.toString().startsWith("kotlin.collections.MutableList"))
        return "mutableListOf()"

    if (this.toString().startsWith("kotlin.collections.List"))
        return "listOf()"

    if (this.toString().startsWith("kotlin.collections.MutableMap"))
        return "mutableMapOf()"

    if (this.toString().startsWith("kotlin.collections.Map"))
        return "mapOf()"

    return "${this}()"
}


fun KmClassifier.classString(): String {
    if (this is KmClassifier.Class) {
        return this.name.replace("/", ".")
    }
    return this.toString()
}

fun Types.allSupertypes(t: TypeMirror): List<TypeMirror> {
    val result = ArrayList<TypeMirror>()
    directSupertypes(t).forEach {
        result.add(it)
        result.addAll(directSupertypes(it))
    }
    return result
}

fun ClassName.isPrimitiveComparable(): Boolean {
    return when (this) {
        UNIT -> false
        BOOLEAN -> true
        BYTE -> true
        SHORT -> true
        INT -> true
        LONG -> true
        CHAR -> true
        FLOAT -> true
        DOUBLE -> true
        STRING -> true
        else -> false
    }
}

val nameHasGenerics = { name: String -> name.contains("<") and name.endsWith(">") }

fun TypeName.bestGuessParametrized(): TypeName {
    return try {
        ClassName.bestGuess(this.toString()).javaToKotlinType()
    } catch (e: IllegalArgumentException) {
        parametrizedTypeNameFrom(this.toString()).javaToKotlinType()
    }
}

fun parametrizedTypeNameFrom(name: String): ParameterizedTypeName {
    if (nameHasGenerics(name)) {
        val classNames = ConcurrentLinkedQueue(name.split("[\\<\\>\\,]".toRegex())
                .filter { it.isNotBlank() }
                .map {
                    it.trim()
                })
        val className = classNames.peek()
        return ClassName.bestGuess(className).parameterizedBy(
                classNames.map { ClassName.bestGuess(it) }
        )
    }

    throw IllegalArgumentException("Not a parametrized class name: $name")

}
