package net.eraga.tools.models

/**
 * Default
 *
 * @param arrayComparing array equality expression, see [ArrayComparing]
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 * Date: 12/07/2021
 * Time: 19:31
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Equals(
    val arrayComparing: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE
)
