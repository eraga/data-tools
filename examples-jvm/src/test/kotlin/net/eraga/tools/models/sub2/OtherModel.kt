package net.eraga.tools.models.sub2

import net.eraga.tools.models.Implement

/**
 * **OtherModel**
 *
 * TODO: Interface OtherModel description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 28/07/2021
 *  Time: 21:38
 */
@Implement.Immutable
@Implement.DTO
interface OtherModel {
    val id: Long
    val name: String
}
