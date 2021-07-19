package net.eraga.tools.models

/**
 * Implement [hashCode] that supports arrays and collections
 *
 * @param arrayComparing select array hashCode expression, see [ArrayComparing]
 *
 * Date: 12/07/2021
 * Time: 19:31
 * @author Klaus Schwartz <mailto:klaus@eraga.net>
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HashCode(
    val arrayComparing: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE
)
