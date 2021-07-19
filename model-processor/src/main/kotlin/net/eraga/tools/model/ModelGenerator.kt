package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import net.eraga.tools.models.Kind
import net.eraga.tools.models.GeneratedClass
import java.io.File
import java.io.Serializable
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import kotlin.NoSuchElementException

/**
 * **ModelGenerator**
 *
 * Generator of boilerplate implementations by Template/Model interface.
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:16
 */
@DelicateKotlinPoetApi("")
@KotlinPoetMetadataPreview
class ModelGenerator(
        metadata: ImmutableSettings,
        kotlinGenerated: String)
//    : AbstractGenerator(metadata, kotlinGenerated) {
//
//
//    override fun generate() {
//        /**
//         * All implementations were disabled, skip generation
//         */
//        if (getLastFromInheritanceChain() == null)
//            return
//
//
//        val immutableInterfaceBuilder = if (immutableInterfaceClass != null) {
//            val builder = TypeSpec.interfaceBuilder(immutableInterfaceClass)
//            if (templateInterfaceClass != null)
//                builder.addSuperinterface(templateInterfaceClass)
//            builder
//        } else
//            null
//
//        val mutableInterfaceBuilder = if (mutableInterfaceClass != null) {
//            val builder = TypeSpec.interfaceBuilder(mutableInterfaceClass)
//            if ((immutableInterfaceClass ?: templateInterfaceClass) != null)
//                builder.addSuperinterface(immutableInterfaceClass ?: templateInterfaceClass!!)
//            builder
//        } else
//            null
//
//        val classBuilder = if (implementationClass != null) {
//            val builder = TypeSpec.classBuilder(implementationClass)
//            if ((mutableInterfaceClass ?: immutableInterfaceClass ?: templateInterfaceClass) != null)
//                builder.addSuperinterface(mutableInterfaceClass ?: immutableInterfaceClass ?: templateInterfaceClass!!)
//            builder
//        } else
//            null
//
//
//        fun builderFromInheritanceChain(): TypeSpec.Builder {
//            return immutableInterfaceBuilder ?: mutableInterfaceBuilder ?: classBuilder!!
//        }
//
//        /**
//         * We annotate only kotlin sources, so this can't be null
//         */
//        val kmClass = element.getAnnotation(Metadata::class.java)?.toImmutableKmClass()!!
//        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)
//
//        if (!metadata.templateSettings.inheritTemplate) {
//            kmClassSpec.superinterfaces.keys.forEach {
//                builderFromInheritanceChain().addSuperinterface(it)
//            }
//        }
//
//        if (implementationClass != null)
//            classNameSpec = kmClass
//
//
//        val constructorBuilder = FunSpec.constructorBuilder()
//
//        var propertyNum = 0
//        for ((name, propertyData) in gatherProperties(element)) {
//            if (propertyData.preventOverride)
//                continue
//
//            val defaultInit = propertyData.defaultInit
////            val getter = propertyData.getter
//            val property = propertyData.typeSpec
//
////            val type = getter.returnType.asTypeName().javaToKotlinType(getter)
//            val type = determinePropertyType(element, propertyData)
//
//            val kotlinProperty = PropertySpec.builder(
//                    name,
//                    type
//            )
////            kotlinProperty.addModifiers(KModifier.VALUE)
//
//            if (!metadata.templateSettings.inheritTemplate && propertyData.isInherited)
//                kotlinProperty.addModifiers(KModifier.OVERRIDE)
//
//            if (getLastFromInheritanceChain() == templateInterfaceClass)
//                kotlinProperty.addModifiers(KModifier.OVERRIDE)
//
//            if (property.isVar)
//                kotlinProperty.mutable(true)
//
//            immutableInterfaceBuilder?.addProperty(kotlinProperty.build())
//
//            if (getLastFromInheritanceChain() == immutableInterfaceClass)
//                kotlinProperty.addModifiers(KModifier.OVERRIDE)
//            mutableInterfaceBuilder?.addProperty(kotlinProperty.mutable(true).build())
//
////            if(getLastFromInheritanceChain() == implementationClass)
//
//            val defaultValue = defaultInit ?: constructorDefaultInitializer(
//                    settings,
//                    type,
//                    propertyData)
//
//            if (getLastFromInheritanceChain() == mutableInterfaceClass)
//                kotlinProperty.addModifiers(KModifier.OVERRIDE)
//
//            if (metadata.constructorVarargPosition() == propertyNum)
//                constructorBuilder.addParameter(
//                        ParameterSpec.builder(
//                                "skipMe",
//                                ProcessingContext.ignoreItClassName
//                        )
//                                .addModifiers(KModifier.VARARG)
//                                .build()
//                )
//
//            constructorBuilder.addParameter(
//                    ParameterSpec.builder(
//                            name,
//                            type
//                    )
//                            .defaultValue(defaultValue)
//                            .build()
//            )
//
//            classBuilder?.addProperty(kotlinProperty.mutable(true).initializer(name).build())
//            propertyNum++
//        }
//
//
//
//
//
//        if (metadata.templateSettings.serializable) {
//            builderFromInheritanceChain().addSuperinterface(Serializable::class)
//        }
//
//        if (metadata.comparableSettings != null) {
//
//        }
//
//
//        val fileBuilder = FileSpec.builder(metadata.elementPackage, metadata.implClassName)
//        if (immutableInterfaceBuilder != null)
//            fileBuilder.addType(immutableInterfaceBuilder.build())
//        if (mutableInterfaceBuilder != null)
//            fileBuilder.addType(mutableInterfaceBuilder.build())
//
//        if (classBuilder != null) {
//            classBuilder
//                    .addModifiers(classModifier)
//                    .primaryConstructor(constructorBuilder.build())
//
//
//
//
//
//            fileBuilder
//                    .addType(classBuilder.build())
//
//            if (metadata.templateSettings.dsl && (classModifier == KModifier.DATA || classModifier == KModifier.OPEN)) {
//                val funBody = CodeBlock.builder()
//                        .add(
//                                """
//val instance = %T()
//instance.init()
//return instance
//""".trimStart('\n'), implementationClass
//                        )
//                        .build()
//
//                val dslFun = FunSpec.builder(metadata.dslFunctionName)
//                        .addParameter(
//                                ParameterSpec.builder(
//                                        "init",
//                                        LambdaTypeName.get(
//                                                receiver = implementationClass,
//                                                returnType = UNIT
//                                        )
//                                ).build()
//                        )
//                        .addCode(funBody)
//                        .returns(getLastFromInheritanceChain()!!)
//
//                fileBuilder.addFunction(dslFun.build())
//            }
//        }
//
//        val file = fileBuilder.build()
//        file.writeTo(File(kotlinGenerated))
//    }
//}
