package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Element
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName


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

fun TypeName.classToInitializer(map:  Map<TypeName, ClassName>): String {
    if (this.toString().contains("Array<"))
        return "arrayOf()"

    return if (this is ParameterizedTypeName &&
            map.containsKey(rawType)) {
        "${map[rawType]!!}()"
    } else {
        "${this}()"
    }
}
