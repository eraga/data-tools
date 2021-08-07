package net.eraga.tools.models

import net.eraga.commons.Copiable
import net.eraga.tools.models.Implement.AnnotationSetting.Target.FIELD
import net.eraga.tools.models.Implement.AnnotationSetting.Target.NONE
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

/**
 * **CategoryModel**
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 07/08/2021
 *  Time: 12:53
 */
interface WithEntityLongId : WithLongId {
    @Implement.AnnotationSetting(Id::class, target = NONE)
    @Implement.AnnotationSetting(Column::class, target = FIELD)
    @get:Id
    @get:Column(nullable = false, unique = true)
    override val id: Long
}

@Implement.Immutable("", implementAnnotations = "javax.persistence.*")
@Implement.Annotate("Category", Entity::class)
interface CategoryModel :
    WithEntityLongId,
    Comparable<CategoryModel>,
    Copiable {

    @get:Column(nullable = false, unique = true)
    val weight: Int
    val name: String

    @Implement.DTO
    interface SomethingLong : Something<DigitalProposalDetails>

    interface Something<T : ProposalDetails> : WithLongId {
        val value: T?
    }
}

open class DigitalProposalDetails(
    val inventoryIds: Collection<Long>,
    val discount: BigDecimal,
) : ProposalDetails()

abstract class ProposalDetails : Serializable {
    val _className: String = this::class.java.canonicalName
}
