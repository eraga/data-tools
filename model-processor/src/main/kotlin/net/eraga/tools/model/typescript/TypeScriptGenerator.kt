package net.eraga.tools.model.typescript

/**
 * **TypeScriptGenerator**
 *
 * TODO: Class TypeScriptGenerator description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 14/07/2021
 *  Time: 14:46
 */
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import me.ntrrgc.tsGenerator.*
import net.eraga.tools.model.*
import net.eraga.tools.model.ProcessingContext.asTypeSpec
import java.beans.Introspector
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.createType

/**
 * TypeScript definition generator.
 *
 * Generates the content of a TypeScript definition file (.d.ts) that
 * covers a set of Kotlin and Java classes.
 *
 * This is useful when data classes are serialized to JSON and
 * handled in a JS or TypeScript web frontend.
 *
 * Supports:
 *  * Primitive types, with explicit int
 *  * Kotlin and Java classes
 *  * Data classes
 *  * Enums
 *  * Any type
 *  * Generic classes, without type erasure
 *  * Generic constraints
 *  * Class inheritance
 *  * Abstract classes
 *  * Lists as JS arrays
 *  * Maps as JS objects
 *  * Null safety, even inside composite types
 *  * Java beans
 *  * Mapping types
 *  * Customizing class definitions via transformers
 *  * Parenthesis are placed only when they are needed to disambiguate
 *
 * @constructor
 *
 * @param rootClasses Initial classes to traverse. Enough definitions
 * will be created to cover them and the types of their properties
 * (and so on recursively).
 *
 * @param mappings Allows to map some JVM types with JS/TS types. This
 * can be used e.g. to map LocalDateTime to JS Date.
 *
 * @param classTransformers Special transformers for certain subclasses.
 * They allow to filter out some classes, customize what methods are
 * exported, how they names are generated and what types are generated.
 *
 * @param ignoreSuperclasses Classes and interfaces specified here will
 * not be emitted when they are used as superclasses or implemented
 * interfaces of a class.
 *
 * @param intTypeName Defines the name integer numbers will be emitted as.
 * By default it's number, but can be changed to int if the TypeScript
 * version used supports it or the user wants to be extra explicit.
 */
@KotlinPoetMetadataPreview
class TypeScriptGenerator(
        rootClasses: Iterable<ImmutableKmClass>,
        private val mappings: Map<ClassName, String> = mapOf(),
        classTransformers: List<ClassTransformer> = listOf(),
        ignoreSuperclasses: Set<ClassName> = setOf(),
        private val intTypeName: String = "number",
        private val voidType: VoidType = VoidType.NULL,
        private val exportPrefix: String = "export"
) {
    private val visitedClasses: MutableSet<TypeName> = java.util.HashSet()
    private val generatedDefinitions = mutableListOf<String>()
    private val pipeline = ClassTransformerPipeline(classTransformers)
    private val ignoredSuperclasses: List<ClassName> = listOf(
            Any::class.asClassName(),
            java.io.Serializable::class.asClassName(),
            ClassName("kotlin.io", "Serializable"),
            Comparable::class.asClassName(),
            java.lang.Comparable::class.asClassName()
    )
            .plus(ignoreSuperclasses)

    init {
        rootClasses.forEach { visitClass(it.asTypeSpec(), it.asClassName()) }
    }

    companion object {
        private val KotlinAnyOrNull = Any::class.createType(nullable = true).asTypeName().asClassName()
    }

    private fun visitClass(klass: TypeSpec, className: ClassName) {
        if (className !in visitedClasses) {
            visitedClasses.add(className)

            /**
             * Don't generate interface for Classes with no package
             */
            if (className.canonicalName.contains("."))
                generatedDefinitions.add(generateDefinition(klass, className))
        }
    }

    private fun formatClassType(klass: TypeSpec, className: TypeName): String {
        visitClass(klass, className.asClassName())
        return className.asClassName().simpleName
    }

    private fun formatKType(
            kType: TypeName,
            isKType: Boolean = false): TypeScriptType {
//        val kType = kclass.className

        val existingMapping = mappings[kType.asClassName()]
        if (existingMapping != null) {
            return TypeScriptType.single(mappings[kType.asClassName()]!!, kType.isNullable, voidType)
        }

        if (kType == Any::class.asClassName()) {
            return TypeScriptType.single("any", kType.isNullable, voidType)
        }

        val classifierTsType: String = when (kType.copy(nullable = false)) {
            Boolean::class.asClassName() -> "boolean"
            String::class.asClassName(),
            Char::class.asClassName() -> "string"
            Int::class.asClassName(),
            Long::class.asClassName(),
            Short::class.asClassName(),
            Byte::class.asClassName() -> intTypeName
            Float::class.asClassName(),
            Double::class.asClassName() -> "number"
            Any::class.asClassName() -> "any"
            else -> {
                if (
                        kType.asClassName() in KOTLIN_ARRAY_INTERFACES ||
                        kType.asTypeSpec().superinterfaces.keys.any { it in KOTLIN_ARRAY_INTERFACES } ||
                        kType.asClassName().implements("Iterable")
                ) {
                    // Use native JS array
                    // Parenthesis are needed to disambiguate complex cases,
                    // e.g. (Pair<string|null, int>|null)[]|null
                    val itemType = when (kType) {
                        // Native Java arrays... unfortunately simple array types like these
                        // are not mapped automatically into kotlin.Array<T> by kotlin-reflect :(
                        IntArray::class.asTypeName() -> Int::class.asTypeName()
                        ShortArray::class.asTypeName() -> Short::class.asTypeName()
                        ByteArray::class.asTypeName() -> Byte::class.asTypeName()
                        CharArray::class.asTypeName() -> Char::class.asTypeName()
                        LongArray::class.asTypeName() -> Long::class.asTypeName()
                        FloatArray::class.asTypeName() -> Float::class.asTypeName()
                        DoubleArray::class.asTypeName() -> Double::class.asTypeName()

                        // Class container types (they use generics)
                        else -> try {
                            kType.typeArguments().single()
                        } catch (_: NoSuchElementException) {
                            KotlinAnyOrNull
                        } catch (_: IllegalArgumentException) {
                            KotlinAnyOrNull
                        }
                    }
                    "${formatKType(itemType).formatWithParenthesis()}[]"
                } else if (kType.asClassName() in setOf(MAP,MUTABLE_MAP) ||
                        kType.asClassName().implements("Map")) {
                    val args = kType.typeArguments()
                    // Use native JS associative object
                    val keyType = formatKType(args[0].asClassName())
                    val valueType = formatKType(args[1].asClassName())
                    "{ [key: ${keyType.formatWithoutParenthesis()}]: ${valueType.formatWithoutParenthesis()} }"
                } else {
                    // Use class name, with or without template parameters
                    val typeSpecs: List<TypeName> = kType.typeArguments()

                    formatClassType(kType.asTypeSpec(), kType) + if (typeSpecs.isNotEmpty()) {
                        "<" + typeSpecs
                                .joinToString(", ") { arg ->
                                    formatKType(arg).formatWithoutParenthesis()
                                } + ">"
                    } else ""
                }
//                else if (isKType) {
//                    kType.simpleName
//                } else {
//                    "UNKNOWN" // giving up
//                }
            }
        }

        return TypeScriptType.single(classifierTsType, kType.isNullable, voidType)
    }

    private fun generateEnum(klass: TypeSpec): String {
        val spec = klass
        return "type ${spec.name} = ${
            spec.enumConstants
                    .map { constant: Any ->
                        constant.toString().toJSString()
                    }
                    .joinToString(" | ")
        };"
    }

    private fun generateInterface(klass: TypeSpec, className: ClassName): String {
        val superclasses = klass.superinterfaces.keys
                .filterNot {
                    it.asClassName() in ignoredSuperclasses
                }

        val extendsString = if (superclasses.isNotEmpty()) {
            " extends " + superclasses.joinToString(", ") {
                formatKType(it).formatWithoutParenthesis()
            }
        } else ""

        val templateParameters = if (className.typeArguments().isNotEmpty()) {
            "<" + klass.typeVariables
                    .map { typeParameter ->
                        val bounds = typeParameter.bounds
                                .filter { it.asClassName().canonicalName != Any::class.asClassName().canonicalName }
                        typeParameter.name + if (bounds.isNotEmpty()) {
                            " extends " + bounds
                                    .map { bound ->
                                        formatKType(bound.asClassName()).formatWithoutParenthesis()
                                    }
                                    .joinToString(" & ")
                        } else {
                            ""
                        }
                    }
                    .joinToString(", ") + ">"
        } else {
            ""
        }

        return "interface ${className.simpleName}$templateParameters$extendsString {\n" +
                klass.propertySpecs
//                        .filter {
//                            it.modifiers.contains(KModifier.PUBLIC)
//                        }
                        .let { propertyList ->
                            pipeline.transformPropertyList(propertyList, klass)
                        }
                        .map { property ->
                            val propertyName = pipeline.transformPropertyName(property.name, property, klass)
                            val propertyType = pipeline.transformPropertyType(property.type, property, klass)

                            val formattedPropertyType = formatKType(propertyType).formatWithoutParenthesis()
                            "    $propertyName: $formattedPropertyType;\n"
                        }
                        .joinToString("") +
                "}"
    }

    private fun isFunctionType(javaType: Type): Boolean {
        return javaType is KCallable<*>
                || javaType.typeName.startsWith("kotlin.jvm.functions.")
                || (javaType is ParameterizedType && isFunctionType(javaType.rawType))
    }

    private fun generateDefinition(klass: TypeSpec, className: ClassName): String {
        return if (klass.isEnum) {
            generateEnum(klass)
        } else {
            generateInterface(klass, className)
        }
    }

    private val export = { it: String -> if (exportPrefix.isNotBlank()) "$exportPrefix $it" else it }

    // Public API:
    val definitionsText: String
        get() = generatedDefinitions.joinToString("\n\n") { export(it) }

    val individualDefinitions: Set<String>
        get() = generatedDefinitions.map { export(it) }.toSet()
}
