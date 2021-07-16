package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * **ProcessingContext**
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:28
 */
@KotlinPoetMetadataPreview
object ProcessingContext {
    lateinit var classInspector: ClassInspector
        private set

    private var processingEnvInternal: ProcessingEnvironment? = null
        set(value) {
            field = value
            classInspector = ElementsClassInspector.create(elements, types)
        }

    fun setProcessingEnv(value: ProcessingEnvironment) {
        processingEnvInternal = value
    }

    private val processingEnv: ProcessingEnvironment
        get() = processingEnvInternal!!


    val types: Types
        get() = processingEnv.typeUtils
    val elements: Elements
        get() = processingEnv.elementUtils

    private val typeSpecs = LinkedHashMap<ClassName, TypeSpec>()

    fun registerTypeSpec(className: ClassName, spec: TypeSpec) {
        typeSpecs[className] = spec
    }

    fun makeTypeSpec(typeName: TypeName): TypeSpec {
        if (typeName is ClassName) {
            return TypeSpec.interfaceBuilder(typeName).build()
        } else if (typeName is ParameterizedTypeName) {
            return TypeSpec.interfaceBuilder(typeName.rawType)
                    .addTypes(typeName.typeArguments.map { it.asTypeSpec() })
                    .build()
        } else {
            return TypeSpec.interfaceBuilder(typeName.toString()).build()
        }
    }

    fun TypeName.asTypeSpec(): TypeSpec {
        val className = asClassName()

        if(typeSpecs.contains(className))
            return typeSpecs[className]!!

        if (className in KOTLIN_PRIMITIVE_TYPES) {
            val typeSpec = makeTypeSpec(className)
            typeSpecs[className] = typeSpec
            return typeSpec
        }

        /**
         * Этой хуйни нет нигде, да и хуй с ней, всё равно оно нахуй не надо
         */
        if(className.isKotlinIntrinsic()) {
            val typeSpec = makeTypeSpec(className)
//            println("WARNING: Kotlin intrinsic ${className.canonicalName}, returning: ${typeSpec.name}")
            typeSpecs[className] = typeSpec
            return typeSpec
        }

        try {
            val typeSpec = Class.forName(
                    className.canonicalName
            )
                    .toTypeSpec(classInspector)
            typeSpecs[className] = typeSpec
            return typeSpec
        } catch (_: ClassNotFoundException){}
        catch (e: IllegalStateException) {
            val typeSpec = makeTypeSpec(className)
//            println("WARNING: Not kotlin Class ${className.canonicalName}, returning: ${typeSpec.name}")
            typeSpecs[className] = typeSpec
            return typeSpec
        }

        val typeElement = elements.getTypeElement(className.canonicalName)

        val kotlinMetaClass = Class.forName("kotlin.Metadata").asSubclass(Annotation::class.java)
        if(typeElement == null) {
            println("WARNING: ${className.canonicalName}: ${typeElement?.getAnnotation(kotlinMetaClass) != null}")
            print("INFO: ")
//            kotlinToJava
            val typeSpec = makeTypeSpec(className)
            typeSpecs[className] = typeSpec
            return typeSpec
        }
        val typeSpec = typeElement.toTypeSpec(classInspector)
        typeSpecs[className] = typeSpec
        return typeSpec
    }
}
