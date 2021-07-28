package net.eraga.tools.models.sub1

import net.eraga.tools.models.Implement
import net.eraga.tools.models.sub2.OtherModel

/**
 * **SomeModel**
 *
 * TODO: Interface SomeModel description
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
interface SomeModel {
    val id: Long
    val name: String

    val otherModel: OtherModel
}
