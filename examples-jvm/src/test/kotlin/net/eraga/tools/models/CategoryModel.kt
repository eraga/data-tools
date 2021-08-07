package net.eraga.tools.models

import net.eraga.commons.Copiable
import net.eraga.tools.models.Implement.AnnotationSetting.Target.NONE
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
    @Implement.AnnotationSetting(Column::class, target = NONE)
    @get:Id
    @get:Column(nullable = false, unique = true)
    override val id: Long
}

@Implement.Immutable(implementAnnotations = "javax.persistence.*")
@Entity
interface CategoryModel :
    WithEntityLongId,
    Comparable<CategoryModel>,
    Copiable {

    val weight: Int
    val name: String
}
