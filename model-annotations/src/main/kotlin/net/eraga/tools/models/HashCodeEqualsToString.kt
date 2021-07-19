package net.eraga.tools.models

/**
 * **HashCodeEqualsToString**
 *
 * Implement [hashCode] and [equals] that supports arrays and collections content comparison
 *
 * Implement [toString] that produces string which can be used as source code.
 *
 * @param arrayComparing select array `compareTo` expression kind, see [ArrayComparing]
 * @param arrayHashCode select array `hashCode` expression kind, see [ArrayComparing]
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 18/07/2021
 *  Time: 01:20
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class HashCodeEqualsToString(
        val arrayComparing: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE,
        val arrayHashCode: ArrayComparing = ArrayComparing.STRUCTURAL_RECURSIVE
)
