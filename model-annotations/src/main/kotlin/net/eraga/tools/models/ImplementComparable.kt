package net.eraga.tools.models
/**
 * Date: 12/07/2021
 * Time: 19:22
 * @author Klaus Schwartz <mailto:klaus@eraga.net>
 */

/**
 * Interface that makes model implement [Comparable].
 *
 * [ImplementComparable] annotation allows to tune generation settings for `override fun compareTo()`.
 *
 * @param order of properties to compare, empty means default order will be used which is order if
 * class superinterfaces. Reverse order can be enforced my `-` sign before property name, e.g. ["-weight", "id"]
 * @param compareAllProperties if set to `false` will only use for comparison properties that are listed in [order]
 * parameter.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ImplementComparable(
    val order: Array<String> = [],
    val compareAllProperties: Boolean = true
)
