package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.classFor
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.model.typescript.ClassNameSpec
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

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
    if (canonicalName.contains("Array")) {
        println(canonicalName)
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

    return kmClass.supertypes.any { it.classifier.classString() == kclass.qualifiedName }
}

@KotlinPoetMetadataPreview
fun ClassName.implements(name: String): Boolean {
    if (this in KOTLIN_ARRAY_INTERFACES)
        return false

    if (implementsInJava(name))
        return true

    val elementsClassInspector = ProcessingContext.classInspector

    if(ClassName.bestGuess(name).canonicalName == canonicalName)
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
