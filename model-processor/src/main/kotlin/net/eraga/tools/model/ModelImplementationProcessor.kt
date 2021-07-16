package net.eraga.tools.model

import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_DTO
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_IMPLEMENTATIONS
import net.eraga.tools.model.ModelImplementationProcessor.Companion.PROCESSED_ANNOTATION_MODEL
import net.eraga.tools.model.typescript.TypeScriptGenerator
import net.eraga.tools.models.*
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
import javax.tools.Diagnostic

/**
 * Date: 27/06/2018
 * Time: 16:21
 */
@DelicateKotlinPoetApi("")
@KotlinPoetMetadataPreview
@SupportedAnnotationTypes(
        PROCESSED_ANNOTATION_MODEL,
        PROCESSED_ANNOTATION_IMPLEMENTATIONS,
        PROCESSED_ANNOTATION_DTO)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(
        "kapt.typescript.generated", "kapt.kotlin.generated"
)
class ModelImplementationProcessor : AbstractProcessor() {
    companion object {
        const val PROCESSED_ANNOTATION_MODEL = "net.eraga.tools.models.ImplementModel"
        const val PROCESSED_ANNOTATION_IMPLEMENTATIONS = "net.eraga.tools.models.Implementations"
        const val PROCESSED_ANNOTATION_DTO = "net.eraga.tools.models.ImplementDTOP"

        val PROCESSED_ANNOTATIONS = arrayOf(
                PROCESSED_ANNOTATION_MODEL,
                PROCESSED_ANNOTATION_DTO
        )
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
        if(annotations.isEmpty())
            return false

        roundEnv.getElementsAnnotatedWith(Implementations::class.java).forEach {
            it.getAnnotation(Implementations::class.java).value.forEach(::println)
        }

        val modelElements = roundEnv.getElementsAnnotatedWith(Implementations::class.java)
                .associateBy({ it as TypeElement }, {
            it.getAnnotation(Implementations::class.java).value
        })

        ProcessingContext.implementedModels = mutableListOf<ModelGenerator>()

        /**
         * Collect information about all models that we are about to implement
         */

        val elements = annotations
                .filter { it.qualifiedName.toString() in PROCESSED_ANNOTATIONS }
                .map { roundEnv.getElementsAnnotatedWith(it) }
                .flatten()
                .filterIsInstance<TypeElement>()

        modelElements.forEach { (typeElement, implementations) ->
            implementations.forEach { settings ->
                ProcessingContext.implementedModels.add(
                        ModelGenerator(
                                typeElement,
                                elements.map { it.qualifiedName.toString() },
                                kotlinGenerated,
                                implementModel = settings
                        ))
            }
        }



        if (elements.any()) {
            File(kotlinGenerated).mkdirs()
            elements.forEach { typeElement ->
                val implementDTO = typeElement.getAnnotation(ImplementDTO::class.java)
                val implementModel = typeElement.getAnnotation(ImplementModel::class.java)
                if (typeElement.kind.isInterface) {
//                    if (implementDTO != null) {
//                        DTOGenerator(typeElement, elements.map { it.qualifiedName.toString() }, kotlinGenerated).run()
//                    }

                    if (implementModel != null) {
                        ProcessingContext.implementedModels.add(
                                ModelGenerator(
                                        typeElement,
                                        elements.map { it.qualifiedName.toString() },
                                        kotlinGenerated,
                                        implementModel = implementModel))
                    } else {
                        println(typeElement.toString())
                    }

                } else {
                    messager.printMessage(
                            Diagnostic.Kind.ERROR, "Only interfaces can be annotated with " +
                            "${implementModel?.javaClass?.simpleName ?: ""} " +
                            "${implementDTO?.javaClass?.simpleName ?: ""} " +
                            " ${typeElement.qualifiedName} is ${typeElement.kind.name}"
                    )
                }
            }
        }

        /**
         * Generate implementations for all models
         */
        ProcessingContext.implementedModels.forEach {
            it.run()
        }

        if(typescriptGenerated != null) {
            val models = ProcessingContext.implementedModels
                    .filter { it.classNameSpec != null }
                    .map { it.classNameSpec!! }
                    .toSet()
            typescript(models, typescriptGenerated!!)
        }

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
}

