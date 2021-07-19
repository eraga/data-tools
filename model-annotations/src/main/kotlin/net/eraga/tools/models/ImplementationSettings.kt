package net.eraga.tools.models

/**
 * **[ImplementationSettings]**
 *
 * Template for model interfaces
 *
 * @param implDsl create dsl function. Defaults to `true`.
 * @param modelSuffix model interface suffix. Defaults to **Model**.
 * @param jpaModelSuffix jpa model interface suffix. Defaults to **JPAModel**.
 * @param implHashCode if true, implements [hashCode].
 * @param implEquals if true, implements [equals].
 * @param implToString if true, implements [toString].
 * @param forceArgNamesInConstructorSkip amount of arguments that will be placed
 * before `vararg _: [IgnoreIt]`, see [IgnoreIt].
 * Disables enforce when value is < 0 or >= than total number of args in constructor. Default `0`.
 * @param implementAnnotations regex pattern to filter out annotations that shouldn't be reimplemented,
 * default is '.*' which matches any annotation.
 */
//@Repeatable
//@Suppress("DEPRECATED_JAVA_ANNOTATION") // suppress deprecation for java.lang.annotation.Repeatable
//@java.lang.annotation.Repeatable(ImplementationTemplates::class)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class ImplementationSettings(
        val implDsl: Boolean = true,
        val modelSuffix: String = "Model",
        val jpaModelSuffix: String = "JPAModel",
        val implHashCode: Boolean = true,
        val implEquals: Boolean = true,
        val implToString: Boolean = true,
        val forceArgNamesInConstructorSkip: Int = 0,
        val implementAnnotations: String = ""
)
