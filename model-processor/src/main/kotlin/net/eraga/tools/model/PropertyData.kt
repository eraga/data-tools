package net.eraga.tools.model

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.lang.model.element.ExecutableElement

/**
 * **PropertyData**
 *
 * TODO: Class PropertyData description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 13/07/2021
 *  Time: 19:49
 */
@KotlinPoetMetadataPreview
class PropertyData(
    val getter: ExecutableElement,
    val typeSpec: ImmutableKmProperty,
    val defaultInit: String?,
    val preventOverride: Boolean,
    val isInherited: Boolean,
    val propertySpec: PropertySpec
)
