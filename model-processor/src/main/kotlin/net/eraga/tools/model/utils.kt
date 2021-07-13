package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isNullable
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.KmClassifier
import org.jetbrains.annotations.Nullable
import java.util.concurrent.ConcurrentLinkedQueue
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

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

public val KOTLIN_ARRAY_INTERFACES: Set<ClassName> = setOf(
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
    val result =  if (type is ParameterizedTypeName) {
        ClassName.bestGuess(this.classifier.classString()).parameterizedBy(
            *type.typeArguments.map { it.javaToKotlinType() }.toTypedArray()
        )
    } else {
        ClassName.bestGuess(this.classifier.classString())
    }

    return if (isNullable) result.copy(nullable = true) else result
}

fun TypeName.javaToKotlinType(type: Element? = null): TypeName {
    val annotation = type?.getAnnotation(Nullable::class.java)

    val result =  if (this is ParameterizedTypeName) {
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

    if (this.toString().contains("Array<"))
        return "arrayOf()"

    if (this.toString().contains("kotlin.collections.MutableList"))
        return "mutableListOf()"

    if (this.toString().contains("kotlin.collections.List"))
        return "listOf()"

    return "${this}()"
}



fun KmClassifier.classString(): String {
    if(this is KmClassifier.Class) {
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
    return when(this) {
        UNIT -> false
        BOOLEAN -> true
        BYTE -> true
        SHORT -> true
        INT -> true
        LONG -> true
        CHAR -> true
        FLOAT -> true
        DOUBLE -> true
        else -> false
    }
}

//fun ClassName.getJavaType(): Boolean {
//    return when(ClassName.bestGuess(this.toString())) {
////        UNIT -> java.lang.Void.TYPE
//        BOOLEAN -> true
//        BYTE -> true
//        SHORT -> true
//        INT -> true
//        LONG -> true
//        CHAR -> true
//        FLOAT -> true
//        DOUBLE -> true
//        else -> false
//    }
//}

val nameHasGenerics = {name: String -> name.contains("<") and name.endsWith(">")}

fun TypeName.bestGuessParametrized(): TypeName {
    return try {
        ClassName.bestGuess(this.toString())
    } catch (e: IllegalArgumentException) {
        parametrizedTypeNameFrom(this.toString())
    }
}

fun parametrizedTypeNameFrom(name: String): ParameterizedTypeName {
    if(nameHasGenerics(name)) {
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
