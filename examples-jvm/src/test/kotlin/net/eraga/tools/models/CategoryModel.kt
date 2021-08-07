package net.eraga.tools.models

import net.eraga.commons.Copiable
import net.eraga.tools.models.Implement.AnnotationSetting.Target.FIELD
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
}
