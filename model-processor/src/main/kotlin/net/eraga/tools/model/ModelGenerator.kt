package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import net.eraga.tools.model.typescript.ClassNameSpec
import net.eraga.tools.model.typescript.TypeScriptGenerator
import net.eraga.tools.models.ImplementModel
import net.eraga.tools.models.ClassKind
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import kotlin.collections.LinkedHashSet

/**
 * **ModelGenerator**
 *
 * TODO: Class ModelGenerator description
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
        element: TypeElement,
        allElements: List<String>,
        kotlinGenerated: String,
        private val implementModel: ImplementModel)
    : AbstractGenerator(element, allElements, kotlinGenerated) {

    override fun generate() {
        val metadata = ModelMetadata(element, implementModel)

        val templateInterfaceClass = if (metadata.modelSettings.inheritTemplate)
            ClassName(
                    metadata.elementPackage,
                    metadata.interfaceClassName
            )
        else
            null

        val immutableInterfaceClass = if (metadata.modelSettings.immutable.classKind == ClassKind.INTERFACE)
            ClassName(
                    metadata.elementPackage,
                    metadata.immutableInterfaceName
            )
        else
            null

        val mutableInterfaceClass =  if (metadata.modelSettings.immutable.classKind == ClassKind.INTERFACE)
            ClassName(
                metadata.elementPackage,
                metadata.mutableInterfaceName
        )
        else
            null

        val implementationClass = if (metadata.modelSettings.kclass.classKind > ClassKind.INTERFACE) {
            ClassName(
                    metadata.elementPackage,
                    metadata.implClassName
            )
        } else
            null

        fun getLastFromInheritanceChain(): ClassName? {
            if(!metadata.modelSettings.inheritTemplate)
                return immutableInterfaceClass ?: mutableInterfaceClass ?: implementationClass
            return templateInterfaceClass ?: immutableInterfaceClass ?: mutableInterfaceClass ?: implementationClass
        }

        /**
         * All implementations were disabled, skip generation
         */
        if(getLastFromInheritanceChain() == null)
            return


        val mutableInterfaceBuilder = if (mutableInterfaceClass != null) {
            val builder = TypeSpec.interfaceBuilder(mutableInterfaceClass)
            if ((immutableInterfaceClass ?: templateInterfaceClass) != null)
                builder.addSuperinterface(immutableInterfaceClass ?: templateInterfaceClass!!)
            builder
        } else
            null

        val immutableInterfaceBuilder = if (immutableInterfaceClass != null) {
            val builder = TypeSpec.interfaceBuilder(immutableInterfaceClass)
            if (templateInterfaceClass != null)
                builder.addSuperinterface(templateInterfaceClass)
            builder
        } else
            null

        val classBuilder = if (implementationClass != null) {
            val builder = TypeSpec.classBuilder(implementationClass)
            if ((mutableInterfaceClass ?: immutableInterfaceClass ?: templateInterfaceClass) != null)
                builder.addSuperinterface(mutableInterfaceClass ?: immutableInterfaceClass ?: templateInterfaceClass!!)
            builder
        } else
            null


        fun builderFromInheritanceChain(): TypeSpec.Builder {
            return immutableInterfaceBuilder ?: mutableInterfaceBuilder ?: classBuilder!!
        }

        if(!metadata.modelSettings.inheritTemplate) {
            val kmClass = element.getAnnotation(Metadata::class.java)?.toImmutableKmClass()
            if (kmClass == null) {
                element.interfaces.forEach {
                    builderFromInheritanceChain().addSuperinterface(it.asTypeName())
                }
            } else {
                val templateSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)
                templateSpec.superinterfaces.keys.forEach {
                    builderFromInheritanceChain().addSuperinterface(it)
                }
                if(implementationClass != null)
                    classNameSpec = kmClass
//                builderFromInheritanceChain().addSuperinterface(ClassInspectorUtil.createClassName(kmClass.name))
            }
        }


        val constructorBuilder = FunSpec.constructorBuilder()
        val propertySet = LinkedHashSet<PropertySpec>()

        for ((name, propertyData) in gatherProperties(element)) {
            if (propertyData.preventOverride)
                continue

            val defaultInit = propertyData.defaultInit
            val getter = propertyData.getter
            val property = propertyData.typeSpec

//            val type = getter.returnType.asTypeName().javaToKotlinType(getter)
            val type = propertyData.propertySpec.type

            val kotlinProperty = PropertySpec.builder(
                    name,
                    type
            )
//            kotlinProperty.addModifiers(KModifier.VALUE)

            if(!metadata.modelSettings.inheritTemplate && propertyData.isInherited)
                kotlinProperty.addModifiers(KModifier.OVERRIDE)

            if(getLastFromInheritanceChain() == templateInterfaceClass)
                kotlinProperty.addModifiers(KModifier.OVERRIDE)

            if(property.isVar)
                kotlinProperty.mutable(true)

            immutableInterfaceBuilder?.addProperty(kotlinProperty.build())

            if(getLastFromInheritanceChain() == immutableInterfaceClass)
                kotlinProperty.addModifiers(KModifier.OVERRIDE)
            mutableInterfaceBuilder?.addProperty(kotlinProperty.mutable(true).build())

            val defaultValue = defaultInit ?: if (type.isNullable) {
                "null"
            } else {
                val returnType = ProcessingContext.types.asElement(getter.returnType)
                if (returnType?.kind == ElementKind.ENUM) {
                    "$type.values()[0]"
                } else {
                    val simpleTypeName = type.toString().split(".").last()

                    if (metadata.primitiveInitializers.containsKey(simpleTypeName)) {
                        metadata.primitiveInitializers[simpleTypeName].toString()
                    } else {
                        if (elements.contains("$type") && returnType is TypeElement) {
                            val meta = ModelMetadata(returnType, implementModel)
                            "${meta.implClassName}()"
                        } else {
                            type.classToInitializer(metadata.classInitializers)
                        }
                    }
                }
            }
            if(getLastFromInheritanceChain() == mutableInterfaceClass)
                kotlinProperty.addModifiers(KModifier.OVERRIDE)

            constructorBuilder.addParameter(
                    ParameterSpec.builder(
                            name,
                            type
                    )
                            .defaultValue(defaultValue)
                            .build()
            )

            propertySet.add(kotlinProperty.initializer(name).build())
        }

        val classModifier = when (metadata.modelSettings.kclass.classKind) {
            ClassKind.OPEN -> {
                KModifier.OPEN
            }
            ClassKind.ABSTRACT -> {
                KModifier.ABSTRACT
            }
            ClassKind.DATA -> {
                KModifier.DATA
            }
            else -> {
                throw IllegalStateException(
                        "incorrect classKind ${metadata.modelSettings.kclass.classKind} for class implementation ${metadata.baseName}"
                )
            }
        }



        if (metadata.modelSettings.serializable) {
            builderFromInheritanceChain().addSuperinterface(Serializable::class)
        }

        if (metadata.comparableSettings != null) {
            val comparableClassName = ClassName.fromKClass(Comparable::class)

            builderFromInheritanceChain().addSuperinterface(
                    comparableClassName.parameterizedBy(
                            getLastFromInheritanceChain()!!
                    )
            )

            val funBodyBuilder =
                    funCompareToBuilder(builderFromInheritanceChain().propertySpecs, metadata.comparableSettings)

            val funBody = funBodyBuilder.build()

            val compareToFun = FunSpec.builder("compareTo")
                    .returns(Int::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(
                            ParameterSpec.builder(
                                    "other",
                                    getLastFromInheritanceChain()!!
                            ).build()
                    )
                    .addCode(funBody)

            builderFromInheritanceChain()
                    .addFunction(compareToFun.build())
        }


        val fileBuilder = FileSpec.builder(metadata.elementPackage, metadata.implClassName)
        if(immutableInterfaceBuilder != null)
            fileBuilder.addType(immutableInterfaceBuilder.build())
        if(mutableInterfaceBuilder != null)
            fileBuilder.addType(mutableInterfaceBuilder.build())

        if (classBuilder != null) {
            classBuilder
                    .addModifiers(classModifier)
                    .primaryConstructor(constructorBuilder.build())
                    .addProperties(propertySet)

            if (metadata.hashCodeSettings != null) {
                val funBodyBuilder = funHashCodeBuilder(classBuilder.propertySpecs, metadata.hashCodeSettings)
                val funBody = funBodyBuilder.build()

                val compareToFun = FunSpec.builder("hashCode")
                        .returns(Int::class)
                        .addModifiers(KModifier.OVERRIDE)
                        .addCode(funBody)

                classBuilder
                        .addFunction(compareToFun.build())
            }

            if (metadata.equalsSettings != null) {
                val funBodyBuilder = funEqualsBuilder(
                        classBuilder.propertySpecs,
                        metadata.implClassName,
                        metadata.equalsSettings
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

                classBuilder
                        .addFunction(compareToFun.build())
            }

            fileBuilder
                    .addType(classBuilder.build())

            if (metadata.modelSettings.dsl && (classModifier == KModifier.DATA || classModifier == KModifier.OPEN)) {
                val funBody = CodeBlock.builder()
                        .add(
                                """
val instance = %T()
instance.init()
return instance
""".trimStart('\n'), implementationClass
                        )
                        .build()

                val dslFun = FunSpec.builder(metadata.dslFunctionName)
                        .addParameter(
                                ParameterSpec.builder(
                                        "init",
                                        LambdaTypeName.get(
                                                receiver = implementationClass,
                                                returnType = UNIT
                                        )
                                ).build()
                        )
                        .addCode(funBody)
                        .returns(getLastFromInheritanceChain()!!)

                fileBuilder.addFunction(dslFun.build())
            }
        }

//        if(classBuilder != null && implementationClass != null) {
//            classNameSpec = implementationClass
//            ProcessingContext.registerTypeSpec(implementationClass, classBuilder.build())
//        }

        val file = fileBuilder.build()
        file.writeTo(File(kotlinGenerated))
    }

    var classNameSpec: ImmutableKmClass? = null

}
