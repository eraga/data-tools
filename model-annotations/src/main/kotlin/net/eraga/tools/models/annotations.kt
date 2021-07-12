package net.eraga.tools.models

import java.util.*
import kotlin.reflect.KClass

/**
 * Indicates annotation processor this interface should have a generated implementation
 *
 * @param dsl create dsl function. Defaults to `true`.
 * @param classKind [Kind.DATA] for `data class` implementation or [Kind.OPEN] for `open class` implementation.
 * @param mutableSuffix mutable model interface suffix. Defaults to **MutableModel**
 * @param modelSuffix model interface suffix. Defaults to **Model**.
 * @param implSuffix implementation class suffix. Defaults to empty string (e.g. **""**).
 * @param serializable should be set to `true` if you want generated classes to implement [Serializable].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ImplementModel(
        val dsl: Boolean = true,
        val classKind: Kind = Kind.DATA,
        val mutableSuffix: String = "MutableModel",
        val modelSuffix: String = "Model",
        val implSuffix: String = "",
        val serializable: Boolean = true
)


/**
 * Provides default value initializer of property in default constructor
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ConstructorInitializer(
    val value: String
)


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


enum class Kind {
    OPEN, DATA
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
