package net.eraga.tools.model.typescript

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType

/**
 * **ClassTransformerPipeline**
 *
 * TODO: Class ClassTransformerPipeline description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 14/07/2021
 *  Time: 14:48
 */


/**
 * Class transformer pipeline.
 *
 * For each method the return value of the first transformer
 * to return not null is used.
 */
internal class ClassTransformerPipeline(val memberTransformers: List<ClassTransformer>): ClassTransformer {

    override fun transformPropertyList(properties: List<PropertySpec>, klass: TypeSpec): List<PropertySpec> {
        var ret = properties
        memberTransformers.forEach { transformer ->
            ret = transformer.transformPropertyList(ret, klass)
        }
        return ret
    }

    override fun transformPropertyName(propertyName: String, property: PropertySpec, klass: TypeSpec): String {
        var ret = propertyName
        memberTransformers.forEach { transformer ->
            ret = transformer.transformPropertyName(ret, property, klass)
        }
        return ret
    }

    override fun transformPropertyType(type: TypeName, property: PropertySpec, klass: TypeSpec): TypeName {
        var ret = type
        memberTransformers.forEach { transformer ->
            ret = transformer.transformPropertyType(ret, property, klass)
        }
        return ret
    }
}
