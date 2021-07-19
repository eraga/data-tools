package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.eraga.tools.models.Implement
import net.eraga.tools.models.Kind
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement

/**
 * **DTOSettings**
 *
 * TODO: Class DTOSettings description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 18/07/2021
 *  Time: 23:04
 */
@KotlinPoetMetadataPreview
class DTOSettings(
        modelElement: TypeElement,
        implementAnnotation: Implement.DTO)
    : AbstractSettings<Implement.DTO>(modelElement, implementAnnotation) {

    var memberModelClassName: ClassName? = null
        private set

    override val implClassName: ClassName

    init {
        implementComparable = true
        implementCloneable = true


        implClassName = when (modelElement.nestingKind) {
            NestingKind.TOP_LEVEL -> {
                ClassName(
                        targetPackage,
                        implementAnnotation.prefix +
                                modelSimpleName.removeSuffix(parentSettings.modelSuffix) +
                                implementAnnotation.suffix + "DTO"

                )
            }
            NestingKind.MEMBER -> {
                println(modelClassName.canonicalName)
                memberModelClassName = modelClassName
                modelClassName = ClassName.bestGuess(targetPackage)
                targetPackage = modelClassName.packageName

                ClassName(
                        targetPackage,
                        modelSimpleName.removeSuffix(parentSettings.modelSuffix) +
                                implementAnnotation.prefix +
                                memberModelClassName!!.simpleName +
                                implementAnnotation.suffix + "DTO"

                )
            }
            else -> throw IllegalArgumentException(
                    "Nesting kind ${modelElement.nestingKind} is not supported by " +
                            "DTO annotation. Remove at class '${modelElement.qualifiedName}'")

        }

        ProcessingContext.registerDTO(modelClassName, this)

        if(implementAnnotation.implementAnnotations.isNotBlank())
            implementAnnotations = implementAnnotation.implementAnnotations
    }


    override fun kclassKind(): Kind {
        return ownSettings.kind
    }
}
