package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.eraga.tools.models.*
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement

/**
 * **ModelMetadata**
 *
 * TODO: Class ModelMetadata description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:48
 */
@KotlinPoetMetadataPreview
class ImmutableSettings(
        modelElement: TypeElement,
        implementAnnotation: Implement.Immutable)
    : AbstractSettings<Implement.Immutable>(modelElement, implementAnnotation) {

    override fun kclassKind(): Kind = ownSettings.kind
    override val implClassName: ClassName
    override val fileBuilder: FileSpec.Builder

    init {
        implClassName = when (modelElement.nestingKind) {
            NestingKind.TOP_LEVEL -> {
                ClassName(
                        targetPackage,
                        implementAnnotation.prefix +
                                modelSimpleName.removeSuffix(parentSettings.modelSuffix) +
                                implementAnnotation.suffix

                )
            }
            else -> throw IllegalArgumentException(
                    "Nesting kind ${modelElement.nestingKind} is not supported by " +
                            "DTO annotation. Remove at class '${modelElement.qualifiedName}'")

        }

        if(implementAnnotation.implementAnnotations.isNotBlank())
            implementAnnotations = implementAnnotation.implementAnnotations

        ProcessingContext.registerImmutable(modelClassName, this)

        fileBuilder = FileSpec.builder(
            implClassName.packageName,
            implClassName.simpleName
        )
    }
}
