package net.eraga.tools.models

/**
 * Indicates annotation processor this interface should have a generated implementation
 *
 * @param dsl create dsl function. Defaults to `true`.
 * @param mutable mutable model interface suffix. Defaults to **MutableModel**
 * @param templateSuffix model interface suffix. Defaults to **Model**.
 * @param kclass implementation class suffix. Defaults to empty string (e.g. **""**).
 * @param serializable should be set to `true` if you want generated classes to implement [Serializable].
 */
//@java.lang.annotation.Repeatable(ImplementModel::class)
//@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ImplementModel(
        val dsl: Boolean = true,
        val mutable: ImplementationMeta = ImplementationMeta(ClassKind.INTERFACE, "MutableModel"),
        val immutable: ImplementationMeta = ImplementationMeta(ClassKind.INTERFACE,"Model"),
        val kclass: ImplementationMeta = ImplementationMeta(ClassKind.DATA,""),
        val templateSuffix: String = "Template",
        val serializable: Boolean = true,
        val inheritTemplate: Boolean = true
)

annotation class ImplementationMeta(
        val classKind: ClassKind,
        val suffix: String = ""
)
