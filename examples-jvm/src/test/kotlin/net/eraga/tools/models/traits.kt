package net.eraga.tools.models

/**
 * **traits**
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 07/08/2021
 *  Time: 12:55
 */
interface WithId<T : Comparable<T>> {
    val id: T
}

interface WithStringId : WithId<String>

interface WithLongId : WithId<Long>
