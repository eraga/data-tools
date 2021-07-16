package net.eraga.tools.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.lang.model.element.TypeElement

/**
 * **DTOGenerator**
 *
 * TODO: Class DTOGenerator description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:02
 */
@KotlinPoetMetadataPreview
class DTOGenerator(
        element: TypeElement,
        allElements: List<String>,
        kotlinGenerated: String)
    : AbstractGenerator(element, allElements, kotlinGenerated) {
    override fun generate() {
        throw UnsupportedOperationException("not implemented") //TODO
    }

}
