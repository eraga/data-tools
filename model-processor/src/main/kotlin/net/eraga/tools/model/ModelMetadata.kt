package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
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
class ModelMetadata(
        element: TypeElement,
        val modelSettings: ImplementModel) {
    val templateClassName: ClassName = ClassName.bestGuess(element.qualifiedName.toString())
    val equalsSettings: ImplementEquals? = element.getAnnotation(ImplementEquals::class.java)
    val hashCodeSettings: ImplementHashCode? = element.getAnnotation(ImplementHashCode::class.java)
    val comparableSettings: ImplementComparable? = element.getAnnotation(ImplementComparable::class.java)

    val interfaceClassName = element.simpleName.toString()
    val baseName = interfaceClassName.removeSuffix(modelSettings.templateSuffix)
    val elementPackage = element.qualifiedName.removeSuffix(".$interfaceClassName").toString()

    val mutableInterfaceName = "$baseName${modelSettings.mutable.suffix}"
    val immutableInterfaceName = "$baseName${modelSettings.immutable.suffix}"
    val implClassName = "$baseName${modelSettings.kclass.suffix}"
    val dslFunctionName = implClassName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val primitiveInitializers =
        primitiveInitializersMap(element.getAnnotation(PrimitiveInitializers::class.java))

    val classInitializers = classInitializersMap(element.getAnnotation(ClassInitializers::class.java))

    fun constructorVarargPosition(): Int {
        return if(modelSettings.forceUseArgNamesInConstructor) modelSettings.forceUseArgNamesInConstructorSkip else -1
    }
}
