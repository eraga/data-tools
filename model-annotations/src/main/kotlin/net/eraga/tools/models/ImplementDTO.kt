package net.eraga.tools.models

/**
 * **ImplementDTO**
 *
 * @param classKind [ClassKind.DATA] for `data class` implementation or [ClassKind.OPEN] for `open class` implementation.
 * @param templateSuffix model interface suffix. Defaults to **Model**.
 * @param implSuffix implementation class suffix. Defaults to `Dto` string (e.g. **""**).
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 18:59
 */
@ImplementModel(
        inheritTemplate = false,
        immutable = ImplementationMeta(ClassKind.NONE),
        mutable = ImplementationMeta(ClassKind.NONE))
annotation class ImplementDTO
//(
//        val classKind: ClassKind = ClassKind.DATA,
//        val templateSuffix: String = "Template",
//        val implSuffix: String = "Dto"
//)
