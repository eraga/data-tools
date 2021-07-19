package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.eraga.tools.models.*
import java.util.*
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

    override fun kclassKind(): Kind = implementAnnotation.kind
    override val implClassName: ClassName

    init {
        println(modelElement.nestingKind)
        implClassName = when (modelElement.nestingKind) {
            NestingKind.TOP_LEVEL -> {
                ClassName(
                        targetPackage,
                        implementAnnotation.prefix +
                                modelSimpleName.removeSuffix(implSettings.modelSuffix) +
                                implementAnnotation.suffix

                )
            }
            else -> throw IllegalArgumentException(
                    "Nesting kind ${modelElement.nestingKind} is not supported by " +
                            "DTO annotation. Remove at class '${modelElement.qualifiedName}'")

        }
    }

}
