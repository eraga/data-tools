package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION
import net.eraga.tools.models.*
import java.io.File
import java.io.Serializable
import java.io.StringWriter
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.NoType
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.NoSuchElementException
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.full.createInstance

/**
 * Date: 27/06/2018
 * Time: 16:21
 */
@KotlinPoetMetadataPreview
@SupportedAnnotationTypes(PROCESSED_ANNOTATION)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ModelImplementationProcessor : AbstractProcessor() {
    companion object {
        const val PROCESSED_ANNOTATION = "net.eraga.tools.models.ImplementModel"

        private fun primitiveInitializersMap(mappings: PrimitiveInitializers?): Map<String, Any?> {
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

        private fun classInitializersMap(initializers: ClassInitializers?): Map<TypeName, ClassName> {
            val model = initializers ?: ClassInitializers::class.constructors.first().call(arrayOf<ClassMapping>())
            val map = HashMap<TypeName, ClassName>()

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
    }

    private val kotlinGenerated: String by lazy { processingEnv.options["kapt.kotlin.generated"].toString() }

    private lateinit var messager: Messager
    private lateinit var typeUtils: Types
    private lateinit var elementsUtil: Elements
    private lateinit var filer: Filer
    private lateinit var elementsClassInspector: ClassInspector
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        typeUtils = processingEnv.typeUtils
        elementsUtil = processingEnv.elementUtils
        filer = processingEnv.filer

        elementsClassInspector = ElementsClassInspector.create(elementsUtil, typeUtils)
    }



    @KotlinPoetMetadataPreview
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = annotations.filter { it.qualifiedName.contentEquals(PROCESSED_ANNOTATION) }
            .map { roundEnv.getElementsAnnotatedWith(it) }
            .flatten()
            .filterIsInstance<TypeElement>()

        if (elements.any()) {
            File(kotlinGenerated).mkdirs()
            elements.forEach { typeElement ->
                if (typeElement.kind.isInterface) {
                    generateImplementation(typeElement, elements.map { it.qualifiedName.toString() })
                } else {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR, "Only interfaces can be annotated with " +
                                "$PROCESSED_ANNOTATION: ${typeElement.qualifiedName} is ${typeElement.kind.name}"
                    )
                }
            }
        }

        return true
    }

    class PropertyData(
        val getter: ExecutableElement,
        val typeSpec: ImmutableKmProperty,
        val defaultInit: String?,
        val preventOverride: Boolean
    )

    private fun gatherProperties(element: TypeElement): Map<String, PropertyData> {
        val getters = LinkedHashMap<String, PropertyData>()

        for (iface in element.interfaces) {
            val realElement = typeUtils.asElement(iface)

            if (realElement is TypeElement) {
                getters.putAll(gatherProperties(realElement))
            }
        }

        val metadata = element.getAnnotation(Metadata::class.java)
        if(metadata == null) {
            println("NOTICE: Skipping ${element.qualifiedName} as it has no Kotlin Metadata")
        } else {
            val kmClass = metadata.toImmutableKmClass()
            val typeSpec = kmClass.toTypeSpec(
                ElementsClassInspector.create(processingEnv.elementUtils, processingEnv.typeUtils)
            )

            var propertyInitMap: Map<String, String>? = null
            var preventOverridesMap: Map<String, Boolean>? = null

            val isConstructorInitializer = {mirror: AnnotationMirror ->
                mirror.annotationType.asElement().simpleName.toString() == ConstructorInitializer::class.java.simpleName}
            val isPreventOverride = {mirror: AnnotationMirror ->
                mirror.annotationType.asElement().simpleName.toString() == PreventOverride::class.java.simpleName}

            val annotationFilterPredicate = { mirror: AnnotationMirror ->
                isPreventOverride(mirror) ||
                        isConstructorInitializer(mirror)
            }

            if (typeSpec.kind == TypeSpec.Kind.INTERFACE) {
                try {
                    val annotatedProps: List<Element>? = element
                        .enclosedElements
                        .first {
                            it.kind == ElementKind.CLASS && it.simpleName.toString() == "DefaultImpls"
                        }
                        .enclosedElements
                        ?.filter {
                            it.simpleName.contains("annotations") &&
                                    it.annotationMirrors.any { mirror ->
                                        annotationFilterPredicate(mirror)
                                    }
                        }

                    propertyInitMap = annotatedProps?.filter {
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

                    preventOverridesMap = annotatedProps?.filter {
                        it.annotationMirrors.any { mirror ->
                            isPreventOverride(mirror)
                        }
                    }?.associateBy({
                        it.simpleName.split("$").first()
                    }, {
                        true
                    })

                } catch (_: NoSuchElementException) {
                }
            }
//        elementsClassInspector.containerData(ClassInspectorUtil.createClassName(kmClass.name), null)
//        elementsClassInspector.declarationContainerFor()

            kmClass.properties.forEach { property ->
                val getter = element.enclosedElements
                    .filterIsInstance<ExecutableElement>().first {
                        it.returnType !is NoType && it.simpleName.toString()
                            .lowercase(Locale.getDefault())
                            .endsWith(property.name.lowercase())
                    }
                getters[property.name] = PropertyData(
                    getter,
                    property,
                    propertyInitMap?.getOrDefault(getter.simpleName.toString(), null),
                    preventOverridesMap?.getOrDefault(getter.simpleName.toString(), null) ?: false
                )
            }
        }

        return getters
    }


    class ModelMetadata(element: TypeElement) {
        val modelSettings = element.getAnnotation(ImplementModel::class.java)!!
        val interfaceClassName = element.simpleName.toString()
        val baseName = interfaceClassName.removeSuffix(modelSettings.modelSuffix)
        val elementPackage = element.qualifiedName.removeSuffix(".$interfaceClassName").toString()

        val mutableInterfaceName = "$baseName${modelSettings.mutableSuffix}"
        val implClassName = "$baseName${modelSettings.implSuffix}"
        val dslFunctionName = implClassName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        val primitiveInitializers = primitiveInitializersMap(element.getAnnotation(PrimitiveInitializers::class.java))
        val classInitializers = classInitializersMap(element.getAnnotation(ClassInitializers::class.java))
    }

    @KotlinPoetMetadataPreview
    private fun generateImplementation(element: TypeElement, elements: List<String>) {

        val metadata = ModelMetadata(element)

        val superInterfaceClass = ClassName(
            metadata.elementPackage,
            metadata.interfaceClassName
        )

        val mutableInterfaceClass = ClassName(
            metadata.elementPackage,
            metadata.mutableInterfaceName
        )

        val implementationClass = ClassName(
            metadata.elementPackage,
            metadata.implClassName
        )


        val mutableInterfaceBuilder = TypeSpec.interfaceBuilder(mutableInterfaceClass)
            .addSuperinterface(superInterfaceClass)

        val classBuilder = TypeSpec.classBuilder(implementationClass)

        val constructorBuilder = FunSpec.constructorBuilder()

        val propertySet = HashSet<PropertySpec>()


        for ((name, propertyData) in gatherProperties(element)) {
            if(propertyData.preventOverride)
                continue

            val defaultInit = propertyData.defaultInit
            val getter = propertyData.getter
            val property = propertyData.typeSpec

//            val type = getter.returnType.asTypeName().javaToKotlinType(getter)
            val type = property.returnType.toKotlinType(getter.returnType.asTypeName())

            val kotlinProperty = PropertySpec.builder(
                name,
                type,
                KModifier.OVERRIDE
            )
                .mutable(true)

            mutableInterfaceBuilder.addProperty(kotlinProperty.build())

            val defaultValue = defaultInit ?: if (type.isNullable) {
                    "null"
                } else {
                    val returnType = typeUtils.asElement(getter.returnType)
                    if (returnType?.kind == ElementKind.ENUM) {
                        "$type.values()[0]"
                    } else {
                        val simpleTypeName = type.toString().split(".").last()

                        if (metadata.primitiveInitializers.containsKey(simpleTypeName)) {
                            metadata.primitiveInitializers[simpleTypeName].toString()
                        } else {
                            if (elements.contains("$type") && returnType is TypeElement) {
                                val meta = ModelMetadata(returnType)
                                "${meta.implClassName}()"
                            } else {
                                type.classToInitializer(metadata.classInitializers)
                            }
                        }
                    }
                }

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

        val classModifier = if (metadata.modelSettings.classKind == Kind.OPEN) {
            KModifier.OPEN
        } else {
            KModifier.DATA
        }

        classBuilder
            .addModifiers(classModifier)
            .addSuperinterface(mutableInterfaceClass)
            .primaryConstructor(constructorBuilder.build())
            .addProperties(propertySet)

        if (metadata.modelSettings.serializable) {
            classBuilder.addSuperinterface(Serializable::class)
        }


        val fileBuilder = FileSpec.builder(metadata.elementPackage, metadata.implClassName)
            .addType(mutableInterfaceBuilder.build())
            .addType(classBuilder.build())

        if (metadata.modelSettings.dsl) {
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
                .returns(superInterfaceClass)

            fileBuilder.addFunction(dslFun.build())
        }

        val file = fileBuilder.build()
        file.writeTo(File(kotlinGenerated))
    }
}
