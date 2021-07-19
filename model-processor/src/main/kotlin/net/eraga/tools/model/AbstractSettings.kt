package net.eraga.tools.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.eraga.tools.models.*
import net.eraga.tools.models.CompareTo
import javax.lang.model.element.TypeElement
import kotlin.reflect.full.createInstance

/**
 * **GeneratorSettings**
 *
 * TODO: Class GeneratorSettings description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 18/07/2021
 *  Time: 13:16
 */
@KotlinPoetMetadataPreview
abstract class AbstractSettings<T>(
        val modelElement: TypeElement,
        val ownSettings: T
) {
    val parentSettings: ImplementationSettings =
            modelElement.getAnnotation(ImplementationSettings::class.java)
                    ?: ImplementationSettings::class.createInstance()

    var implementAnnotations: String = parentSettings.implementAnnotations
        protected set

    val primitiveInitializers =
            primitiveInitializersMap(modelElement.getAnnotation(PrimitiveInitializers::class.java))

    val classInitializers =
            classInitializersMap(modelElement.getAnnotation(ClassInitializers::class.java))

    var modelClassName: ClassName = ClassName.bestGuess(modelElement.qualifiedName.toString())
        protected set

    val modelSimpleName: String
        get() = modelClassName.simpleName

    var targetPackage = modelElement.qualifiedName.removeSuffix(".$modelSimpleName").toString()
        protected set

    abstract val implClassName: ClassName

    /**
     * Decided by ImplementationSettings
     */
    val implementEquals: Boolean = parentSettings.implEquals
    val implementHashCode: Boolean = parentSettings.implHashCode
    val implementToString: Boolean = parentSettings.implToString

    val equalsSettings: Equals = modelElement.getAnnotation(Equals::class.java) ?: Equals::class.createInstance()
    val hashCodeSettings: HashCode = modelElement.getAnnotation(HashCode::class.java)
            ?: HashCode::class.createInstance()
    val comparableSettings: CompareTo = modelElement.getAnnotation(CompareTo::class.java)
            ?: CompareTo::class.createInstance()

    /**
     * Decided by presence of corresponding supertypes
     */
    var implementComparable: Boolean = modelElement.implements(Comparable::class)
        protected set

    var implementCloneable: Boolean = modelElement.implements(Cloneable::class)
        protected set

//    TODO:
//     abstract val implementDeepCloneable: Boolean
//    TODO:
//     val implementSerializable: Boolean

    val constructorVarargPosition: Int = parentSettings.forceArgNamesInConstructorSkip

    abstract fun kclassKind(): Kind

    val classModifier = when (kclassKind()) {
        Kind.OPEN_CLASS -> KModifier.OPEN
        Kind.FINAL_CLASS -> KModifier.FINAL
        Kind.ABSTRACT -> KModifier.ABSTRACT
        Kind.DATA -> KModifier.DATA
        else -> {
            throw IllegalStateException(
                    "incorrect classKind ${kclassKind()} for class implementation ${modelElement.simpleName}"
            )
        }
    }

    /**
     * Generated spec
     */
    lateinit var typeSpec: TypeSpec
}
