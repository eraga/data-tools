package net.eraga.tools.models

/**
 * **Implementations**
 *
 * Repeatable doesn't work in Kotlin,
 * this is workaround to provide multiple implementations
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:48
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Implementations(
        vararg val value: ImplementModel
)
