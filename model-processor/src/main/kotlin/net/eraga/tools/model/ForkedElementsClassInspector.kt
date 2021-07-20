package net.eraga.tools.model

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.common.Visibility
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.*
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.ConstructorData
import com.squareup.kotlinpoet.metadata.specs.ContainerData
import com.squareup.kotlinpoet.metadata.specs.EnumEntryData
import com.squareup.kotlinpoet.metadata.specs.FieldData
import com.squareup.kotlinpoet.metadata.specs.FileData
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.DEFAULT
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.specs.MethodData
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.JVM_NAME
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.filterOutNullabilityAnnotations
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.*
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.type.*
import javax.lang.model.util.AbstractTypeVisitor6
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KClass

private typealias ElementsModifier = javax.lang.model.element.Modifier

/**
 * An [Elements]-based implementation of [ClassInspector].
 */
@KotlinPoetMetadataPreview
class ForkedElementsClassInspector private constructor(
        private val elements: Elements,
        private val types: Types
) : ClassInspector {
    private val typeElementCache = ConcurrentHashMap<ClassName, Optional<TypeElement>>()
    private val methodCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<ExecutableElement>>()
    private val variableElementCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<VariableElement>>()
    private val jvmNameType = elements.getTypeElement(JVM_NAME.canonicalName)
    private val jvmNameName = ElementFilter.methodsIn(jvmNameType.enclosedElements)
            .first { it.simpleName.toString() == "name" }

    private fun lookupTypeElement(className: ClassName): TypeElement? {
        return typeElementCache.getOrPut(className) {
            elements.getTypeElement(className.canonicalName).toOptional()
        }.nullableValue
    }

    override val supportsNonRuntimeRetainedAnnotations: Boolean = true

    override fun declarationContainerFor(className: ClassName): ImmutableKmDeclarationContainer {
        val typeElement = lookupTypeElement(className)
                ?: error("No type element found for: $className.")

        val metadata = typeElement.getAnnotation(Metadata::class.java)
        return when (val kotlinClassMetadata = metadata.readKotlinClassMetadata()) {
            is KotlinClassMetadata.Class -> kotlinClassMetadata.toImmutableKmClass()
            is KotlinClassMetadata.FileFacade -> kotlinClassMetadata.toImmutableKmPackage()
            else -> TODO("Not implemented yet: ${kotlinClassMetadata.javaClass.simpleName}")
        }
    }

    override fun isInterface(className: ClassName): Boolean {
        if (className in ClassInspectorUtil.KOTLIN_INTRINSIC_INTERFACES) {
            return true
        }
        return lookupTypeElement(className)?.kind == INTERFACE
    }

    private fun TypeElement.lookupField(fieldSignature: JvmFieldSignature): VariableElement? {
        val signatureString = fieldSignature.asString()
        return variableElementCache.getOrPut(this to signatureString) {
            ElementFilter.fieldsIn(enclosedElements)
                    .find { signatureString == it.jvmFieldSignature(types) }.toOptional()
        }.nullableValue
    }

    private fun lookupMethod(
            className: ClassName,
            methodSignature: JvmMethodSignature,
            elementFilter: (Iterable<Element>) -> List<ExecutableElement>
    ): ExecutableElement? {
        return lookupTypeElement(className)?.lookupMethod(methodSignature, elementFilter)
    }

    private fun TypeElement.lookupMethod(
            methodSignature: JvmMethodSignature,
            elementFilter: (Iterable<Element>) -> List<ExecutableElement>
    ): ExecutableElement? {
        val signatureString = methodSignature.asString()
        return methodCache.getOrPut(this to signatureString) {
            elementFilter(enclosedElements)
                    .find { signatureString == it.jvmMethodSignature(types) }.toOptional()
        }.nullableValue
    }

    private fun VariableElement.jvmModifiers(isJvmField: Boolean): Set<JvmFieldModifier> {
        return modifiers.mapNotNullTo(mutableSetOf()) {
            when {
                it == ElementsModifier.TRANSIENT -> TRANSIENT
                it == ElementsModifier.VOLATILE -> VOLATILE
                !isJvmField && it == ElementsModifier.STATIC -> JvmFieldModifier.STATIC
                else -> null
            }
        }
    }

    private fun VariableElement.annotationSpecs(): List<AnnotationSpec> {
        @Suppress("DEPRECATION")
        return filterOutNullabilityAnnotations(
                annotationMirrors.map { AnnotationSpec.get(it) }
        )
    }

    private fun ExecutableElement.jvmModifiers(): Set<JvmMethodModifier> {
        return modifiers.mapNotNullTo(mutableSetOf()) {
            when (it) {
                ElementsModifier.SYNCHRONIZED -> SYNCHRONIZED
                ElementsModifier.STATIC -> STATIC
                ElementsModifier.DEFAULT -> DEFAULT
                else -> null
            }
        }
    }

    private fun ExecutableElement.annotationSpecs(): List<AnnotationSpec> {
        @Suppress("DEPRECATION")
        return filterOutNullabilityAnnotations(
                annotationMirrors.map {
                        it.asAnnotationSpec()
                }.flatten()
        )
    }

    private fun ExecutableElement.exceptionTypeNames(): List<TypeName> {
        @Suppress("DEPRECATION")
        return thrownTypes.map { it.asTypeName() }
    }

    override fun enumEntry(enumClassName: ClassName, memberName: String): EnumEntryData {
        val enumType = lookupTypeElement(enumClassName)
                ?: error("No type element found for: $enumClassName.")
        val enumTypeAsType = enumType.asType()
        val member = typeElementCache.getOrPut(enumClassName.nestedClass(memberName)) {
            ElementFilter.typesIn(enumType.enclosedElements)
                    .asSequence()
                    .filter { types.isSubtype(enumTypeAsType, it.superclass) }
                    .find { it.simpleName.contentEquals(memberName) }.toOptional()
        }.nullableValue
        val declarationContainer = member?.getAnnotation(Metadata::class.java)
                ?.toImmutableKmClass()

        val entry = ElementFilter.fieldsIn(enumType.enclosedElements)
                .asSequence()
                .find { it.simpleName.contentEquals(memberName) }
                ?: error("Could not find the enum entry for: $enumClassName")

        return EnumEntryData(
                declarationContainer = declarationContainer,
                annotations = entry.annotationSpecs()
        )
    }

    private fun VariableElement.constantValue(): CodeBlock? {
        return constantValue?.let(ClassInspectorUtil::codeLiteralOf)
    }

    override fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean {
        return lookupMethod(className, methodSignature, ElementFilter::methodsIn) != null
    }

    /**
     * Detects whether [this] given method is overridden in [type].
     *
     * Adapted and simplified from AutoCommon's private
     * [MoreElements.getLocalAndInheritedMethods] methods implementations for detecting
     * overrides.
     */
    private fun ExecutableElement.isOverriddenIn(type: TypeElement): Boolean {
        val methodMap = LinkedHashMultimap.create<String, ExecutableElement>()
        type.getAllMethods(MoreElements.getPackage(type), methodMap)
        // Find methods that are overridden using `Elements.overrides`. We reduce the performance
        // impact by:
        //   (a) grouping methods by name, since a method cannot override another method with a
        //       different name. Since we know the target name, we just inspect the methods with
        //       that name.
        //   (b) making sure that methods in ancestor types precede those in descendant types,
        //       which means we only have to check a method against the ones that follow it in
        //       that order. Below, this means we just need to find the index of our target method
        //       and compare against only preceding ones.
        val methodList = methodMap.asMap()[simpleName.toString()]?.toList()
                ?: return false
        val signature = jvmMethodSignature(types)
        return methodList.asSequence()
                .filter { it.jvmMethodSignature(types) == signature }
                .take(1)
                .any { elements.overrides(this, it, type) }
    }

    /**
     * Add to [methodsAccumulator] the instance methods from [this] that are visible to code in
     * the package [pkg]. This means all the instance methods from [this] itself and all
     * instance methods it inherits from its ancestors, except private methods and
     * package-private methods in other packages. This method does not take overriding into
     * account, so it will add both an ancestor method and a descendant method that overrides
     * it. [methodsAccumulator] is a multimap from a method name to all of the methods with
     * that name, including methods that override or overload one another. Within those
     * methods, those in ancestor types always precede those in descendant types.
     *
     * Adapted from AutoCommon's private [MoreElements.getLocalAndInheritedMethods] methods'
     * implementations, before overridden methods are stripped.
     */
    private fun TypeElement.getAllMethods(
            pkg: PackageElement,
            methodsAccumulator: SetMultimap<String, ExecutableElement>
    ) {
        for (superInterface in interfaces) {
            MoreTypes.asTypeElement(superInterface).getAllMethods(pkg, methodsAccumulator)
        }
        if (superclass.kind != TypeKind.NONE) {
            // Visit the superclass after superinterfaces so we will always see the implementation of a
            // method after any interfaces that declared it.
            MoreTypes.asTypeElement(superclass).getAllMethods(pkg, methodsAccumulator)
        }
        for (method in ElementFilter.methodsIn(enclosedElements)) {
            if (ElementsModifier.STATIC !in method.modifiers &&
                    ElementsModifier.FINAL !in method.modifiers &&
                    ElementsModifier.PRIVATE !in method.modifiers &&
                    method.isVisibleFrom(pkg)
            ) {
                methodsAccumulator.put(method.simpleName.toString(), method)
            }
        }
    }

    private fun ExecutableElement.isVisibleFrom(pkg: PackageElement): Boolean {
        // We use Visibility.ofElement rather than [MoreElements.effectiveVisibilityOfElement]
        // because it doesn't really matter whether the containing class is visible. If you
        // inherit a public method then you have a public method, regardless of whether you
        // inherit it from a public class.
        return when (Visibility.ofElement(this)) {
            Visibility.PRIVATE -> false
            Visibility.DEFAULT -> MoreElements.getPackage(this) == pkg
            else -> true
        }
    }

    override fun containerData(
            declarationContainer: ImmutableKmDeclarationContainer,
            className: ClassName,
            parentClassName: ClassName?
    ): ContainerData {
        val typeElement: TypeElement = lookupTypeElement(className) ?: error("No class found for: $className.")
        val isCompanionObject = when (declarationContainer) {
            is ImmutableKmClass -> {
                declarationContainer.isCompanionObject
            }
            is ImmutableKmPackage -> {
                false
            }
            else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
        }

        // Should only be called if parentName has been null-checked
        val classIfCompanion by lazy(NONE) {
            if (isCompanionObject && parentClassName != null) {
                lookupTypeElement(parentClassName)
                        ?: error("No class found for: $parentClassName.")
            } else {
                typeElement
            }
        }

        val propertyData = declarationContainer.properties
                .asSequence()
                .filter { it.isDeclaration }
                .filterNot { it.isSynthesized }
                .associateWith { property ->
                    val isJvmField = ClassInspectorUtil.computeIsJvmField(
                            property = property,
                            classInspector = this,
                            isCompanionObject = isCompanionObject,
                            hasGetter = property.getterSignature != null,
                            hasSetter = property.setterSignature != null,
                            hasField = property.fieldSignature != null
                    )

                    val fieldData = property.fieldSignature?.let fieldDataLet@{ fieldSignature ->
                        // Check the field in the parent first. For const/static/jvmField elements, these only
                        // exist in the parent and we want to check that if necessary to avoid looking up a
                        // non-existent field in the companion.
                        val parentModifiers = if (isCompanionObject && parentClassName != null) {
                            classIfCompanion.lookupField(fieldSignature)?.jvmModifiers(isJvmField).orEmpty()
                        } else {
                            emptySet()
                        }

                        val isStatic = JvmFieldModifier.STATIC in parentModifiers

                        // TODO we looked up field once, let's reuse it
                        val classForOriginalField = typeElement.takeUnless {
                            isCompanionObject &&
                                    (property.isConst || isJvmField || isStatic)
                        } ?: classIfCompanion

                        val field = classForOriginalField.lookupField(fieldSignature)
                                ?: return@fieldDataLet FieldData.SYNTHETIC
                        val constant = if (property.hasConstant) {
                            val fieldWithConstant = classIfCompanion.takeIf { it != typeElement }?.let {
                                if (it.kind.isInterface) {
                                    field
                                } else {
                                    // const properties are relocated to the enclosing class
                                    it.lookupField(fieldSignature)
                                            ?: return@fieldDataLet FieldData.SYNTHETIC
                                }
                            } ?: field
                            fieldWithConstant.constantValue()
                        } else {
                            null
                        }

                        val jvmModifiers = field.jvmModifiers(isJvmField) + parentModifiers

                        FieldData(
                                annotations = field.annotationSpecs(),
                                isSynthetic = false,
                                jvmModifiers = jvmModifiers.filterNotTo(mutableSetOf()) {
                                    // JvmField companion objects don't need JvmStatic, it's implicit
                                    isCompanionObject && isJvmField && it == JvmFieldModifier.STATIC
                                },
                                constant = constant
                        )
                    }

                    val getterData = property.getterSignature?.let { getterSignature ->
                        val method = classIfCompanion.lookupMethod(getterSignature, ElementFilter::methodsIn)
                        method?.methodData(
                                typeElement = typeElement,
                                hasAnnotations = property.getterFlags.hasAnnotations,
                                jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                                        ?.lookupMethod(getterSignature, ElementFilter::methodsIn)
                                        ?: method
                        )
                                ?: return@let MethodData.SYNTHETIC
                    }

                    val setterData = property.setterSignature?.let { setterSignature ->
                        val method = classIfCompanion.lookupMethod(setterSignature, ElementFilter::methodsIn)
                        method?.methodData(
                                typeElement = typeElement,
                                hasAnnotations = property.setterFlags.hasAnnotations,
                                jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                                        ?.lookupMethod(setterSignature, ElementFilter::methodsIn)
                                        ?: method,
                                knownIsOverride = getterData?.isOverride
                        )
                                ?: return@let MethodData.SYNTHETIC
                    }

                    val annotations = mutableListOf<AnnotationSpec>()
                    if (property.hasAnnotations) {
                        property.syntheticMethodForAnnotations?.let { annotationsHolderSignature ->
                            val method = typeElement.lookupMethod(annotationsHolderSignature, ElementFilter::methodsIn)
                                    ?: return@let MethodData.SYNTHETIC
                            annotations += method.annotationSpecs()
                        }
                    }

                    val synthetic = property.syntheticMethodForAnnotations?.name
                    if (synthetic != null) {
                        val annotationsHack = try {
                            typeElement.enclosedElements
                                    .filterIsInstance<ExecutableElement>()
                                    .first { synthetic == it.simpleName.toString() }
                        } catch (e: Exception) {
                            try {
                                typeElement.enclosedElements
                                        .first { it.simpleName.toString() == "DefaultImpls" }
                                        .enclosedElements
                                        .filterIsInstance<ExecutableElement>()
                                        .first { synthetic == it.simpleName.toString() }
                            } catch (e: Exception) {
                                val annos = typeElement.enclosedElements
                                        .map { it.simpleName }.toString()
                                //                        giving up
                                println("   not found among: $annos")
                                null
                            }
                        }
                        if (annotationsHack != null) {
                            annotations += annotationsHack.annotationSpecs().filter {
                                it.typeName.toString() != "java.lang.Deprecated" ||
                                        it.typeName.toString() != "java.lang.Override"

                            }
//                                if(property.name == "id") {
//                                    val oldSize = annotations.size
//                                    println("${typeElement.simpleName}.${property.name} " +
//                                            "was $oldSize, " +
//                                            "now ${annotations.size}, " +
//                                            "found ${annotationsHack.annotationMirrors.size}")
//                                    println("Found new annotations: ${annotationsHack.annotationMirrors.map { it.annotationType.toString() }}")
//                                    println("Full annotations list: ${annotations.map { it.typeName }}")
//                                }
                        }

                    }

                    // If a field is static in a companion object, remove the modifier and add the annotation
                    // directly on the top level. Otherwise this will generate `@field:JvmStatic`, which is
                    // not legal
                    var finalFieldData = fieldData
                    fieldData?.jvmModifiers?.let {
                        if (isCompanionObject && JvmFieldModifier.STATIC in it) {
                            finalFieldData = fieldData.copy(
                                    jvmModifiers = fieldData.jvmModifiers
                                            .filterNotTo(LinkedHashSet()) { it == JvmFieldModifier.STATIC }
                            )
                            annotations += AnnotationSpec.builder(
                                    JVM_STATIC
                            ).build()
                        }
                    }

                    PropertyData(
                            annotations = annotations,
                            fieldData = finalFieldData,
                            getterData = getterData,
                            setterData = setterData,
                            isJvmField = isJvmField
                    )
                }

        val methodData = declarationContainer.functions.associateWith { kmFunction ->
            val signature = kmFunction.signature
            if (signature != null) {
                val method = typeElement.lookupMethod(signature, ElementFilter::methodsIn)
                method?.methodData(
                        typeElement = typeElement,
                        hasAnnotations = kmFunction.hasAnnotations,
                        jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                                ?.lookupMethod(signature, ElementFilter::methodsIn)
                                ?: method
                )
                        ?: return@associateWith MethodData.SYNTHETIC
            } else {
                MethodData.EMPTY
            }
        }

        when (declarationContainer) {
            is ImmutableKmClass -> {
                val constructorData = declarationContainer.constructors.associateWith { kmConstructor ->
                    if (declarationContainer.isAnnotation || declarationContainer.isValue) {
                        //
                        // Annotations are interfaces in bytecode, but kotlin metadata will still report a
                        // constructor signature
                        //
                        // Inline classes have no constructors at runtime
                        //
                        return@associateWith ConstructorData.EMPTY
                    }
                    val signature = kmConstructor.signature
                    if (signature != null) {
                        val constructor = typeElement.lookupMethod(signature, ElementFilter::constructorsIn)
                                ?: return@associateWith ConstructorData.EMPTY
                        ConstructorData(
                                annotations = if (kmConstructor.hasAnnotations) {
                                    constructor.annotationSpecs()
                                } else {
                                    emptyList()
                                },
                                parameterAnnotations = constructor.parameters.indexedAnnotationSpecs(),
                                isSynthetic = false,
                                jvmModifiers = constructor.jvmModifiers(),
                                exceptions = constructor.exceptionTypeNames()
                        )
                    } else {
                        ConstructorData.EMPTY
                    }
                }
                return ClassData(
                        declarationContainer = declarationContainer,
                        className = className,
                        annotations = if (declarationContainer.hasAnnotations) {
                            ClassInspectorUtil.createAnnotations {
                                @Suppress("DEPRECATION")
                                addAll(typeElement.annotationMirrors.map { AnnotationSpec.get(it) })
                            }
                        } else {
                            emptyList()
                        },
                        properties = propertyData,
                        constructors = constructorData,
                        methods = methodData
                )
            }
            is ImmutableKmPackage -> {
                // There's no flag for checking if there are annotations, so we just eagerly check in this
                // case. All annotations on this class are file: site targets in source. This includes
                // @JvmName.
                var jvmName: String? = null
                val fileAnnotations = ClassInspectorUtil.createAnnotations(FILE) {
                    addAll(
                            typeElement.annotationMirrors.map {
                                if (it.annotationType == jvmNameType) {
                                    val nameValue = requireNotNull(it.elementValues[jvmNameName]) {
                                        "No name property found on $it"
                                    }
                                    jvmName = nameValue.value as String
                                }
                                @Suppress("DEPRECATION")
                                AnnotationSpec.get(it)
                            }
                    )
                }
                return FileData(
                        declarationContainer = declarationContainer,
                        annotations = fileAnnotations,
                        properties = propertyData,
                        methods = methodData,
                        className = className,
                        jvmName = jvmName
                )
            }
            else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
        }
    }

//    fun <A : Annotation> annotationData(
//            typeElement: TypeElement,
//            property: ImmutableKmProperty,
//            annotation: A
//    ): A? {
//        typeElement.getAnnotation(annotation::class.java)
//        if (property.hasAnnotations) {
//            property.syntheticMethodForAnnotations?.let { annotationsHolderSignature ->
//                val method = typeElement.lookupMethod(annotationsHolderSignature, ElementFilter::methodsIn)
//                        ?: return@let MethodData.SYNTHETIC
//                val result = method.annotationMirrors.firstOrNull { it ->
//                    annotation == it
//
//                    val element = annotation.annotationType.asElement() as TypeElement
//                    val builder = AnnotationSpec.builder(element.asClassName()).tag(annotation)
//                    for (executableElement in annotation.elementValues.keys) {
//                        val member = CodeBlock.builder()
//                        val visitor = AnnotationSpec.Visitor(member)
//                        val name = executableElement.simpleName.toString()
//                        member.add("%L = ", name)
//                        val value = annotation.elementValues[executableElement]!!
//                        value.accept(visitor, name)
//                        builder.addMember(member.build())
//                    }
//
//                }
//                if(result != null)
//                    return result as A
//
//                return null
//            }
//        }
//    }


    private fun List<VariableElement>.indexedAnnotationSpecs(): Map<Int, Collection<AnnotationSpec>> {
        return withIndex().associate { (index, parameter) ->
            index to ClassInspectorUtil.createAnnotations { addAll(parameter.annotationSpecs()) }
        }
    }

    private fun ExecutableElement.methodData(
            typeElement: TypeElement,
            hasAnnotations: Boolean,
            jvmInformationMethod: ExecutableElement = this,
            knownIsOverride: Boolean? = null
    ): MethodData {
        return MethodData(
                annotations = if (hasAnnotations) annotationSpecs() else emptyList(),
                parameterAnnotations = parameters.indexedAnnotationSpecs(),
                isSynthetic = false,
                jvmModifiers = jvmInformationMethod.jvmModifiers(),
                isOverride = knownIsOverride?.let { it } ?: isOverriddenIn(typeElement),
                exceptions = exceptionTypeNames()
        )
    }

    public companion object {
        /** @return an [Elements]-based implementation of [ClassInspector]. */
        @JvmStatic
        @KotlinPoetMetadataPreview
        public fun create(elements: Elements, types: Types): ClassInspector {
            return ForkedElementsClassInspector(elements, types)
        }

        private val JVM_STATIC = JvmStatic::class.asClassName()
    }
}

/**
 * Simple `Optional` implementation for use in collections that don't allow `null` values.
 *
 * TODO: Make this an inline class when inline classes are stable.
 */
private data class Optional<out T : Any>(val nullableValue: T?)

private fun <T : Any> T?.toOptional(): Optional<T> = Optional(
        this
)

/*
 * Adapted from
 * - https://github.com/Takhion/kotlin-metadata/blob/e6de126575ad6ca10b093129b7c30d000c9b0c37/lib/src/main/kotlin/me/eugeniomarletti/kotlin/metadata/jvm/JvmDescriptorUtils.kt
 * - https://github.com/Takhion/kotlin-metadata/pull/13
 */

/**
 * For reference, see the [JVM specification, section 4.2](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2).
 *
 * @return the name of this [Element] in its "internal form".
 */
internal val Element.internalName: String
    get() = when (this) {
        is TypeElement -> {
            when (nestingKind) {
                NestingKind.TOP_LEVEL ->
                    qualifiedName.toString().replace('.', '/')
                NestingKind.MEMBER ->
                    enclosingElement.internalName + "$" + simpleName
                NestingKind.LOCAL, NestingKind.ANONYMOUS ->
                    error("Unsupported nesting $nestingKind")
                null ->
                    error("Unsupported, nestingKind == null")
            }
        }
        is QualifiedNameable -> qualifiedName.toString().replace('.', '/')
        else -> simpleName.toString()
    }

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
@Suppress("unused")
internal val NoType.descriptor: String
    get() = "V"

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal val DeclaredType.descriptor: String
    get() = "L" + asElement().internalName + ";"

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal val PrimitiveType.descriptor: String
    get() = when (this.kind) {
        TypeKind.BYTE -> "B"
        TypeKind.CHAR -> "C"
        TypeKind.DOUBLE -> "D"
        TypeKind.FLOAT -> "F"
        TypeKind.INT -> "I"
        TypeKind.LONG -> "J"
        TypeKind.SHORT -> "S"
        TypeKind.BOOLEAN -> "Z"
        else -> error("Unknown primitive type $this")
    }

/**
 * @see [JvmDescriptorTypeVisitor]
 */
internal fun TypeMirror.descriptor(types: Types): String =
        accept(JvmDescriptorTypeVisitor, types)

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal fun WildcardType.descriptor(types: Types): String =
        types.erasure(this).descriptor(types)

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal fun TypeVariable.descriptor(types: Types): String =
        types.erasure(this).descriptor(types)

/**
 * @return the "field descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal fun ArrayType.descriptor(types: Types): String =
        "[" + componentType.descriptor(types)

/**
 * @return the "method descriptor" of this type.
 * @see [JvmDescriptorTypeVisitor]
 */
internal fun ExecutableType.descriptor(types: Types): String {
    val parameterDescriptors = parameterTypes.joinToString(separator = "") { it.descriptor(types) }
    val returnDescriptor = returnType.descriptor(types)
    return "($parameterDescriptors)$returnDescriptor"
}

/**
 * Returns the JVM signature in the form "$Name$MethodDescriptor", for example: `equals(Ljava/lang/Object;)Z`.
 *
 * Useful for comparing with [JvmMethodSignature].
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
internal fun ExecutableElement.jvmMethodSignature(types: Types): String {
    return "$simpleName${asType().descriptor(types)}"
}

/**
 * Returns the JVM signature in the form "$Name:$FieldDescriptor", for example: `"value:Ljava/lang/String;"`.
 *
 * Useful for comparing with [JvmFieldSignature].
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
internal fun VariableElement.jvmFieldSignature(types: Types): String {
    return "$simpleName:${asType().descriptor(types)}"
}

/**
 * When applied over a type, it returns either:
 * - a "field descriptor", for example: `Ljava/lang/Object;`
 * - a "method descriptor", for example: `(Ljava/lang/Object;)Z`
 *
 * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
 */
internal object JvmDescriptorTypeVisitor : AbstractTypeVisitor6<String, Types>() {
    override fun visitNoType(t: NoType, types: Types): String = t.descriptor
    override fun visitDeclared(t: DeclaredType, types: Types): String = t.descriptor
    override fun visitPrimitive(t: PrimitiveType, types: Types): String = t.descriptor

    override fun visitArray(t: ArrayType, types: Types): String = t.descriptor(types)
    override fun visitWildcard(t: WildcardType, types: Types): String = t.descriptor(types)
    override fun visitExecutable(t: ExecutableType, types: Types): String = t.descriptor(types)
    override fun visitTypeVariable(t: TypeVariable, types: Types): String = t.descriptor(types)

    override fun visitNull(t: NullType, types: Types): String = visitUnknown(
            t, types
    )

    override fun visitError(t: ErrorType, types: Types): String = visitUnknown(
            t, types
    )

    override fun visitUnknown(t: TypeMirror, types: Types): String = error("Unsupported type $t")
}
