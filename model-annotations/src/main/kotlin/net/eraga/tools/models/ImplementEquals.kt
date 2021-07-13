package net.eraga.tools.models

/**
 * Date: 12/07/2021
 * Time: 19:31
 * @author Klaus Schwartz <mailto:klaus@eraga.net>
 */

/**
 * Default
 *
 * @param arrayComparing array equality expression, see [ArrayComparing]
 */
annotation class ImplementEquals(
    val arrayComparing: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE
)
