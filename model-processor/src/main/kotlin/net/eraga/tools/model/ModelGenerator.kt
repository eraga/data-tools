package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import net.eraga.tools.models.ImplementModel
import net.eraga.tools.models.ClassKind
import net.eraga.tools.models.GeneratedClass
import net.eraga.tools.models.IgnoreIt
import java.io.File
import java.io.Serializable
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import kotlin.NoSuchElementException
import kotlin.collections.LinkedHashSet

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
        element: TypeElement,
        allElements: List<String>,
        kotlinGenerated: String,
        val implementModel: ImplementModel)
    : AbstractGenerator(element, allElements, kotlinGenerated) {
    val metadata = ModelMetadata(element, implementModel)

    private val templateInterfaceClass = if (metadata.modelSettings.inheritTemplate)
        ClassName(
                metadata.elementPackage,
                metadata.interfaceClassName
        )
    else
        null

    private val immutableInterfaceClass = if (metadata.modelSettings.immutable.classKind == ClassKind.INTERFACE)
        ClassName(
                metadata.elementPackage,
                metadata.immutableInterfaceName
        )
    else
        null

    private val mutableInterfaceClass =  if (metadata.modelSettings.mutable.classKind == ClassKind.INTERFACE)
        ClassName(
                metadata.elementPackage,
                metadata.mutableInterfaceName
        )
    else
        null

    private val implementationClass = if (metadata.modelSettings.kclass.classKind > ClassKind.INTERFACE) {
        ClassName(
                metadata.elementPackage,
                metadata.implClassName
        )
    } else
        null

    private fun getLastFromInheritanceChain(inheritTemplate: Boolean = metadata.modelSettings.inheritTemplate): ClassName? {
        if(!inheritTemplate)
            return immutableInterfaceClass ?: mutableInterfaceClass ?: implementationClass
        return templateInterfaceClass ?: immutableInterfaceClass ?: mutableInterfaceClass ?: implementationClass
    }

    override fun generate() {
        /**
         * All implementations were disabled, skip generation
         */
        if(getLastFromInheritanceChain() == null)
            return



        val immutableInterfaceBuilder = if (immutableInterfaceClass != null) {
            val builder = TypeSpec.interfaceBuilder(immutableInterfaceClass)
            if (templateInterfaceClass != null)
                builder.addSuperinterface(templateInterfaceClass)
            builder
        } else
            null

        val mutableInterfaceBuilder = if (mutableInterfaceClass != null) {
            val builder = TypeSpec.interfaceBuilder(mutableInterfaceClass)
            if ((immutableInterfaceClass ?: templateInterfaceClass) != null)
                builder.addSuperinterface(immutableInterfaceClass ?: templateInterfaceClass!!)
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

        /**
         * We annotate only kotlin sources, so this can't be null
         */
        val kmClass = element.getAnnotation(Metadata::class.java)?.toImmutableKmClass()!!
        val kmClassSpec = kmClass.toTypeSpec(ProcessingContext.classInspector)

        if(!metadata.modelSettings.inheritTemplate) {
            kmClassSpec.superinterfaces.keys.forEach {
                builderFromInheritanceChain().addSuperinterface(it)
            }
            if(implementationClass != null)
                classNameSpec = kmClass
        }


        val constructorBuilder = FunSpec.constructorBuilder()
        val propertySet = LinkedHashSet<PropertySpec>()

        var propertyNum = 0
        for ((name, propertyData) in gatherProperties(element)) {
            if (propertyData.preventOverride)
                continue

            val defaultInit = propertyData.defaultInit
//            val getter = propertyData.getter
            val property = propertyData.typeSpec

//            val type = getter.returnType.asTypeName().javaToKotlinType(getter)
            val type = if(propertyData.propertySpec.type.toString() == "error.NonExistentClass") {
//                propertyData.typeSpec.syntheticMethodForAnnotations
//                val generatedClass = getter.getAnnotation(GeneratedClass::class.java)
                val isConstructorInitializer = { mirror: AnnotationMirror ->
                    mirror.annotationType.asElement().simpleName.toString() == GeneratedClass::class.java.simpleName
                }
                val annotatedProps: List<Element>? = element
                        .enclosedElements
                        .first {
                            it.kind == ElementKind.CLASS && it.simpleName.toString() == "DefaultImpls"
                        }
                        .enclosedElements
                        ?.filter {
                            it.simpleName.contains("annotations") &&
                                    it.annotationMirrors.any { mirror ->
                                        isConstructorInitializer(mirror)
                                    }
                        }


                val propertyInitMap = annotatedProps?.filter {
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

                if(propertyInitMap!!.values.first().isNotBlank())
                    ClassName.bestGuess(propertyInitMap.values.first())
                else
                    propertyData.propertySpec.type
            } else {
                try {
                    ProcessingContext.implementedModels
                            .first { it.metadata.templateClassName == propertyData.propertySpec.type }
                            .getLastFromInheritanceChain()!!
                } catch (_: NoSuchElementException) {
                    propertyData.propertySpec.type
                }
            }

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
                val returnTypeSpec = propertyData.propertySpec.type.asTypeSpec()
                if (returnTypeSpec.isEnum) {
                    try {
                        "${returnTypeSpec.name}.${returnTypeSpec.enumConstants.keys.first()}"
                    } catch (e: NoSuchElementException) {
                        "$type.values()[0]"
                    }
                } else {
                    val simpleTypeName = type.toString().split(".").last()

                    if (metadata.primitiveInitializers.containsKey(simpleTypeName)) {
                        metadata.primitiveInitializers[simpleTypeName].toString()
                    } else {
                        try {
                            val typeModel = ProcessingContext.implementedModels.first {
                                it.getLastFromInheritanceChain() == type
                            }
                            val meta = typeModel.metadata
                            "${meta.implClassName}()"
                        } catch(e: NoSuchElementException) {
                            val classInitializer = type.classToInitializer(metadata.classInitializers)
                            if(classInitializer.contains("NonExistentClass")) {
                                println("$classInitializer == $type")
                                type.toString()
                            }
                            classInitializer
                        }
                    }
                }
            }
            if(getLastFromInheritanceChain() == mutableInterfaceClass)
                kotlinProperty.addModifiers(KModifier.OVERRIDE)

            if(metadata.constructorVarargPosition() == propertyNum)
                constructorBuilder.addParameter(
                        ParameterSpec.builder(
                                "skipMe",
                                IgnoreIt::class.asTypeName()
                        )
                                .addModifiers(KModifier.VARARG)
                                .build()
                )

            constructorBuilder.addParameter(
                    ParameterSpec.builder(
                            name,
                            type
                    )
                            .defaultValue(defaultValue)
                            .build()
            )

            propertySet.add(kotlinProperty.initializer(name).build())
            propertyNum++
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
                            getLastFromInheritanceChain(inheritTemplate = false)!!
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
                                    getLastFromInheritanceChain(inheritTemplate = false)!!
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

        val file = fileBuilder.build()
        file.writeTo(File(kotlinGenerated))
    }

    var classNameSpec: ImmutableKmClass? = null

}
