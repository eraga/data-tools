package net.eraga.tools.model

import net.eraga.tools.models.*
import java.util.*
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
class ImmutableSettings(
        element: TypeElement,
        templateSettings: ImplementationSettings
)
//    : AbstractSettings<ImplementationSettings>(element, templateSettings) {
//
//    val baseName = modelSimpleName.removeSuffix(templateSettings.modelSuffix)
//    val mutableInterfaceName = "$baseName${templateSettings.mutable.suffix}"
//    val immutableInterfaceName = "$baseName${templateSettings.immutable.suffix}"
////    val implClassName = "$baseName${templateSettings.kclass.suffix}"
//
//    val dslFunctionName = implClassName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
//
//
//    override fun constructorVarargPosition(): Int {
//        /**
//         * Kotlin data classes don't support constructor varargs
//         */
//        if(implementAnnotation.kclass.kind == Kind.DATA)
//            return -1
//
//        return if(implementAnnotation.forceUseArgNamesInConstructor) implementAnnotation.forceArgNamesInConstructorSkip else -1
//    }
//}
