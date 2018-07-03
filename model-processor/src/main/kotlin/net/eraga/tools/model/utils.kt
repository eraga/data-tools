package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

fun TypeName.javaToKotlinType(type: Element? = null): TypeName {
    val annotation = type?.getAnnotation(Nullable::class.java)

    val result =  if (this is ParameterizedTypeName) {
        ParameterizedTypeName.get(
                rawType.javaToKotlinType() as ClassName,
                *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
        )
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

    return if (annotation != null) result.asNullable() else result
}

fun TypeName.classToInitializer(map:  Map<TypeName, ClassName>): String {
    if (this.toString().contains("Array"))
        return "arrayOf()"

    return if (this is ParameterizedTypeName &&
            map.containsKey(rawType)) {
        "${map[rawType]!!}()"
    } else {
        "${this}()"
    }
}
