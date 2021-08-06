package net.eraga.tools.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

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
    val defaultInit: String?,
    val constructorInit: Boolean,
    val preventOverride: Boolean,
    val isInherited: Boolean,
    var propertySpec: PropertySpec,
    val additionalAnnotations: List<AnnotationSpec>,
    val isFinal: Boolean
)
