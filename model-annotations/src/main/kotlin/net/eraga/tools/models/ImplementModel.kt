package net.eraga.tools.models

/**
 * Indicates annotation processor this interface should have a generated implementation
 *
 * @param dsl create dsl function. Defaults to `true`.
 * @param classKind [Kind.DATA] for `data class` implementation or [Kind.OPEN] for `open class` implementation.
 * @param mutableSuffix mutable model interface suffix. Defaults to **MutableModel**
 * @param templateSuffix model interface suffix. Defaults to **Model**.
 * @param implSuffix implementation class suffix. Defaults to empty string (e.g. **""**).
 * @param serializable should be set to `true` if you want generated classes to implement [Serializable].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ImplementModel(
    val dsl: Boolean = true,
    val classKind: Kind = Kind.DATA,
    val mutableSuffix: String = "MutableModel",
    val templateSuffix: String = "Template",
    val immutableSuffix: String = "Model",
    val implSuffix: String = "",
    val serializable: Boolean = true
)
