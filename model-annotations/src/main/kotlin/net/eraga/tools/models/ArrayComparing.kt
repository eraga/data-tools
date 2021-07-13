package net.eraga.tools.models

/**
 * **ArrayTreatment**
 *
 * How to perform array equals, hashCode, etc.
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 04:30
 */
enum class ArrayComparing {
    /**
     * use equals, default in java and kotlin data classes
     */
    DEFAULT,

    /**
     * arrays will be compared structurally, but if it is a multidimensional array (array of arrays),
     * the subarrays will be compared referentially (through equals() on arrays)
     */
    STRUCTURAL,

    /**
     * same as [STRUCTURAL] including subarrays (like Lists)
     */
    STRUCTURAL_RECURSIVE
}
