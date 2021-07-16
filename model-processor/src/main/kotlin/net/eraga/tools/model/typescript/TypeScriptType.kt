package net.eraga.tools.model.typescript

import me.ntrrgc.tsGenerator.VoidType

/**
 * **TypeScriptType**
 *
 * TODO: Class TypeScriptType description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 14/07/2021
 *  Time: 14:49
 */
internal class TypeScriptType private constructor(val types: List<String>) {
    companion object {
        fun single(type: String, nullable: Boolean, voidType: VoidType): TypeScriptType {
            return TypeScriptType(listOf(type)).let {
                if (nullable) {
                    it or TypeScriptType(listOf(voidType.jsTypeName))
                } else {
                    it
                }
            }
        }

        fun union(types: List<String>): TypeScriptType {
            return TypeScriptType(types)
        }
    }

    infix fun or(other: TypeScriptType): TypeScriptType {
        val combinedTypes = (this.types + other.types).distinct()

        return TypeScriptType(if ("any" in combinedTypes) {
            listOf("any")
        } else {
            combinedTypes
        })
    }

    fun formatWithParenthesis(): String {
        if (types.size == 1) {
            return types.single()
        } else {
            return "(" + this.formatWithoutParenthesis() + ")"
        }
    }

    fun formatWithoutParenthesis(): String {
        return types.joinToString(" | ")
    }
}
