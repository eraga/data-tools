package net.eraga.tools.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_JPA
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_DTO
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_IMMUTABLE
import net.eraga.tools.model.typescript.TypeScriptGenerator
import net.eraga.tools.models.*
import net.eraga.tools.models.implement.DTOs
import net.eraga.tools.models.implement.Immutables
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.collections.HashSet

/**
 * Date: 27/06/2018
 * Time: 16:21
 */
@DelicateKotlinPoetApi("")
@KotlinPoetMetadataPreview
@SupportedAnnotationTypes(
        PROCESSED_ANNOTATION_IMMUTABLE,
        PROCESSED_ANNOTATION_DTO,
        PROCESSED_ANNOTATION_JPA)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        "kapt.typescript.generated",
        "kapt.kotlin.generated"
)
class ModelImplementationProcessor : AbstractProcessor() {
    override fun getSupportedOptions(): Set<String> {
        return setOf(
                "kapt.typescript.generated",
                "kapt.kotlin.generated"
        )
    }

    companion object {
        const val PROCESSED_ANNOTATION_IMMUTABLE = "net.eraga.tools.models.Implement.Immutable"
        const val PROCESSED_ANNOTATION_DTO = "net.eraga.tools.models.Implement.DTO"
        const val PROCESSED_ANNOTATION_JPA = "net.eraga.tools.models.Implement.JPAEntity"
    }

    private val kotlinGenerated: String by lazy { processingEnv.options["kapt.kotlin.generated"]!! }
    private val typescriptGenerated: String? by lazy { processingEnv.options["kapt.typescript.generated"] }

    private lateinit var messager: Messager
    private lateinit var typeUtils: Types
    private lateinit var elementsUtil: Elements
    private lateinit var filer: Filer
    private lateinit var elementsClassInspector: ClassInspector
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        ProcessingContext.setProcessingEnv(processingEnv)

        messager = processingEnv.messager
        typeUtils = processingEnv.typeUtils
        elementsUtil = processingEnv.elementUtils
        filer = processingEnv.filer

        elementsClassInspector = ElementsClassInspector.create(elementsUtil, typeUtils)
    }


    @KotlinPoetMetadataPreview
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isEmpty())
            return false
        val modelKeys = HashSet<TypeElement>()
//        val implementTemplateElements = roundEnv.getElementsAnnotatedWith(ImplementationTemplates::class.java)
//                .associateBy({ modelKeys.add(it as TypeElement); it }, {
//                    it.getAnnotation(ImplementationTemplates::class.java).value.toMutableList()
//                })
        /**
         * Getting grouped annotations
         */
        val dtoElements = roundEnv.getElementsAnnotatedWith(DTOs::class.java)
                .associateBy({ modelKeys.add(it as TypeElement); it }, {
                    it.getAnnotation(DTOs::class.java).value.toMutableList()
                })
                .toMutableMap()

        val immutableElements = roundEnv.getElementsAnnotatedWith(Immutables::class.java)
                .associateBy({ modelKeys.add(it as TypeElement); it }, {
                    it.getAnnotation(Immutables::class.java).value.toMutableList()
                })
                .toMutableMap()


        /**
         * Getting single annotations
         */
        roundEnv.getElementsAnnotatedWith(Implement.DTO::class.java)
                .filterIsInstance<TypeElement>()
                .forEach {
                    val list = dtoElements
                            .getOrDefault(it, mutableListOf())

                    list.add(it.getAnnotation(Implement.DTO::class.java))
                    dtoElements[it] = list
                }

        roundEnv.getElementsAnnotatedWith(Implement.Immutable::class.java)
                .filterIsInstance<TypeElement>()
                .forEach {
                    val list = immutableElements
                            .getOrDefault(it, mutableListOf())

                    list.add(it.getAnnotation(Implement.Immutable::class.java))
                    immutableElements[it] = list
                }

        /**
         * Creating generators
         */
        dtoElements.forEach { (typeElement, implementations) ->
            val settings =  implementations.map { settings ->
                DTOSettings(typeElement, settings)
            }
            ProcessingContext.implementations.addAll(settings)

            ProcessingContext.implementedModels.add(DTOGenerator(settings))
        }

        immutableElements.forEach { (typeElement, implementations) ->
            val settings =  implementations.map { settings ->
                ImmutableSettings(typeElement, settings)
            }
            ProcessingContext.implementations.addAll(settings)

            ProcessingContext.implementedModels.add(ImmutableGenerator(settings))
        }

        /**
         * Generate implementations for all models
         */
        if (ProcessingContext.implementedModels.isNotEmpty()) {
            val path = File(kotlinGenerated)
            path.mkdirs()

            ProcessingContext.implementedModels.forEach {
                it.run()
                it.generatedSpecs.forEach { spec ->
                    spec.writeTo(path)
                }
            }

            if (ProcessingContext.implementedModels.any { generator ->
                        generator.listOfImplementations.any {
                            it.constructorVarargPosition >= 0
                        }
                    })
                generateIgnoreItClass()
        }

//        if (typescriptGenerated != null) {
//            val models = ProcessingContext.implementedModels
//                    .filter { it.classNameSpec != null }
//                    .map { it.classNameSpec!! }
//                    .toSet()
//            typescript(models, typescriptGenerated!!)
//        }

        return true
    }

    private fun typescript(classes: Set<ImmutableKmClass>, path: String) {
        val tsDir = File(path).apply { mkdirs() }

        TypeScriptGenerator(
                rootClasses = classes,
                mappings = mapOf(
                        LocalDateTime::class.asClassName() to "Date",
                        LocalDate::class.asClassName() to "Date",
                        UUID::class.asClassName() to "string"
                )
        ).definitionsText.apply {
            FileWriter(File("${tsDir.absolutePath}/index.ts")).use {
                it.write(this)
            }
        }
    }

    private fun generateIgnoreItClass() {
        val ignoreItClassName = ProcessingContext.ignoreItClassName
        FileSpec.builder(ignoreItClassName.packageName, ignoreItClassName.canonicalName)
                .addType(TypeSpec.classBuilder(ignoreItClassName).build())
                .build()
                .writeTo(File(kotlinGenerated))
    }
}

