package net.eraga.tools.model.typescript

/**
 * **extensions**
 *
 * TODO: Class extensions description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 14/07/2021
 *  Time: 14:50
 */
internal fun String.toJSString(): String {
    return "\"${this
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\"", "\\\"")
    }\""
}
