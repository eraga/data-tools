package net.eraga.tools.models

/**
 * Date: 12/07/2021
 * Time: 19:31
 * @author Klaus Schwartz <mailto:klaus@eraga.net>
 */

/**
 * Implementation of [hashCode] that supports arrays and collections
 *
 * @param arrayComparing array hashCode expression, see [ArrayComparing]
 */
annotation class ImplementHashCode(
    val arrayComparing: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE
)
