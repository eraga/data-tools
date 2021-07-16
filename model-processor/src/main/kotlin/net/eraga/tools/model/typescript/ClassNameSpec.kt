package net.eraga.tools.model.typescript

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import net.eraga.tools.model.ProcessingContext.asTypeSpec

/**
 * **ClassNameTypeSpec**
 *
 * TODO: Class ClassNameTypeSpec description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 14/07/2021
 *  Time: 19:44
 */
@KotlinPoetMetadataPreview
class ClassNameSpec(
        val className: ClassName,
        private val originatingElementsHolder: OriginatingElementsHolder
) {
    constructor(
            className: ClassName,
            typeSpec: TypeSpec
    ) : this(className = className, originatingElementsHolder = typeSpec)

    constructor(
            className: ClassName,
            propertySpec: PropertySpec
    ) : this(className = className, originatingElementsHolder = propertySpec)

    val isProperty = originatingElementsHolder is PropertySpec
    val isType = originatingElementsHolder is TypeSpec

    val forceTypeSpec: TypeSpec
        get() {
            return if (isType) originatingElementsHolder as TypeSpec else {
                propSpec.type.asTypeSpec().toBuilder()
                        .addTypeVariables(propSpec.typeVariables).build()
            }
        }

    val name: String
    get() {
        return if (isType) typeSpec.name.toString() else {
            propSpec.type.asTypeSpec().name.toString()
        }
    }

    val typeSpec: TypeSpec
        get() {
            return originatingElementsHolder as TypeSpec
        }

    val propSpec: PropertySpec
        get() = originatingElementsHolder as PropertySpec

    val superinterfaces: Set<TypeName>
        get() {
            val spec = if (isType) typeSpec else {
                propSpec.type.asTypeSpec()
            }
            return spec.superinterfaces.keys
        }

    val kind: TypeSpec.Kind
        get() {
            return if (isType) typeSpec.kind else {
                propSpec.type.asTypeSpec().kind
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassNameSpec) return false

        if (className.canonicalName != other.className.canonicalName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + originatingElementsHolder.hashCode()
        return result
    }
}
