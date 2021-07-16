package net.eraga.tools.models

import java.util.*
import kotlin.reflect.KClass


/**
 * Prevents override of annotated property
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class PreventOverride()


@Retention(AnnotationRetention.SOURCE)
annotation class ClassMapping(
        val source: KClass<*>,
        val target: KClass<*>
)


/**
 * Provides default constructor initializers for [ImplementModel] processor
 *
 * @list  of interface initializers enclosed in [ClassMapping]
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ClassInitializers(
        val list: Array<ClassMapping>
)

/**
 * Provides primitive types initializers for [ImplementModel] processor
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PrimitiveInitializers(
        val Boolean: Boolean = false,
        val String: String = "",
        val Byte: Byte = 0,
        val Short: Short = 0,
        val Int: Int = 0,
        val Long: Long = 0L,
        val Float: Float = 0.0f,
        val Double: Double = 0.0
)


enum class ClassKind {
    /**
     * Don't implement default class
     */
    NONE,

    /**
     * Implement interface
     */
    INTERFACE,

    /**
     * Implement open class
     */
    OPEN,

    /**
     * Implement data class
     */
    DATA,

    /**
     * Implement abstract class
     */
    ABSTRACT;

}

@Suppress("unused")
fun ClassInitializers.classMappingDefaults(): Map<KClass<*>, KClass<*>> {
    return mapOf(
           Pair(Set::class, HashSet::class),
           Pair(MutableSet::class, HashSet::class),
           Pair(Map::class, HashMap::class),
           Pair(MutableMap::class, HashMap::class),
           Pair(Iterable::class, ArrayList::class),
           Pair(MutableIterable::class, ArrayList::class),
           Pair(Collection::class, ArrayList::class),
           Pair(MutableCollection::class, ArrayList::class),
           Pair(List::class, ArrayList::class),
           Pair(MutableList::class, ArrayList::class)
   )
}
