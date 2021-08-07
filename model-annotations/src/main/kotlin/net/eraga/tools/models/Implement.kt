package net.eraga.tools.models

import net.eraga.tools.models.implement.*
import kotlin.reflect.KClass

/**
 * **Implement**
 *
 * Grouping Annotation for implementation definition annotations.
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 18/07/2021
 *  Time: 21:52
 */
annotation class Implement {
    /**
     * **ImplementDTO**
     *
     * Implements immutable class like [Immutable] but does not inherit from model.
     * Always adds `DTO` to the end of class name.
     *
     * If Model is annotated with [Immutable] or [JPAEntity], immutable implementation will have update methods
     * for each corresponding DTO.
     *
     * @param prefix of implemented class, default empty
     * @param suffix of implemented class, default empty
     * @param kind of implementation class, default [Kind.FINAL_CLASS] see [Kind]
     * @param propsForceNull if true, will convert all properties to nullable
     * @param propsDefaultOmit if true, will implement only overridden properties
     * @param implementAnnotations override of [ImplementationSettings.implementAnnotations]
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(DTOs::class)
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class DTO(
        val prefix: String = "",
        val suffix: String = "",
        val kind: Kind = Kind.FINAL_CLASS,
        val propsForceNull: Boolean = false,
        val propsDefaultOmit: Boolean = false,
        val implementAnnotations: String = ""
    )

    /**
     * **ImplementImmutable**
     *
     * Implements immutable class with [equals], [hashCode], [toString], `copy` and `deepCopy` methods
     * and model constructor with enforced use of arguments.
     *
     * @param prefix of implemented class, default empty
     * @param suffix of implemented class, default empty
     * @param kind of implementation class, default [Kind.OPEN_CLASS] see [Kind]
     * @param implementAnnotations override of [ImplementationSettings.implementAnnotations]
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(Immutables::class)
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Immutable(
        val prefix: String = "Immutable",
        val suffix: String = "",
        val kind: Kind = Kind.FINAL_CLASS,
        val implementAnnotations: String = ""
    )

    /**
     * **ImplementEntity**
     *
     * TODO: Implement JPA @Entity from Model. Will add @Entity annotation.
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(JPAEntities::class)
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class JPAEntity(
        val prefix: String = "",
        val suffix: String = "",
        val kind: Kind = Kind.FINAL_CLASS,
        val implementAnnotations: String = ""
    )


    /**
     * Don't implement property for [Implement] implementation
     *
     * @param in simple name of [Implement] Class for which to omit implementation,
     * empty value makes it to be omitted for all implementations
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(Omitted::class)
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Omit(val `in`: String = "")

    annotation class Omitted(vararg val value: Omit)


    /**
     * Provides default value initializer of property in default constructor
     *
     * @param with initializer string, will be written as is, required parameter.
     * @param in which implementation, defaults to empty which matches all implementations
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(Initialized::class)
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Init(
        val with: String,
        val `in`: String = ""
//            TODO:
//            vararg val scopes: InitScope = []
    )

    annotation class Initialized(vararg val value: Init)

    /**
     * Skips default init and moves property to constructor
     *
     * @param in which implementation, defaults to empty which matches all implementations
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(NotInitialized::class)
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class NoInit(
        val `in`: String = ""
    )

    annotation class NotInitialized(vararg val value: NoInit)
//    enum class InitScope {
//        PROPERTY, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR
//    }

    /**
     * When certain implementation property requires to be annotated with one that
     * can't be used on model interface property or not desired in all implementations this
     *
     * @param in implementation class simple name of which implementation to annotate
     * @param with annotation class
     * @param args array of named arguments and values to be passed to e.g.
     * `["argument1 = value1", "argument2 = Value(2)"]`
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(Annotates::class)
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Annotate(
        val `in`: String = "",
        val with: KClass<out Annotation>,
        vararg val args: String = [],
    )

    annotation class Annotates(vararg val value: Annotate)

    /**
     * TODO Doc
     */
    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(AnnotationSettings::class)
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class AnnotationSetting(
        vararg val classes: KClass<out Annotation>,
        val target: Target = Target.INHERIT,
//        val `in`: String = ".*"
    ) {
        @Suppress("unused")
        enum class Target(
            val siteTarget: String
        ) {
            /**
             * Remove annotation site-target
             */
            NONE(""),

            /**
             * Don't change annotation site-target
             */
            INHERIT(""),

            FILE("file"),

            /**
             * (annotations with this target are not visible to Java)
             */
            PROPERTY("property"),

            FIELD("field"),

            /**
             * (property getter)
             */
            GET("get"),

            /**
             * (property setter)
             */
            SET("set"),

            /**
             * (receiver parameter of an extension function or property)
             */
            RECEIVER("receiver"),

            /**
             * (constructor parameter)
             */
            PARAM("param"),

            /**
             * (property setter parameter)
             */
            SETPARAM("setparam"),

            /**
             * (the field storing the delegate instance for a delegated property)
             */
            DELEGATE("delegate"),
        }
    }


    annotation class AnnotationSettings(vararg val value: AnnotationSetting)

    @Repeatable
    @Suppress("DEPRECATED_JAVA_ANNOTATION")
    @java.lang.annotation.Repeatable(Narrows::class)
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Narrow(
        val `in`: String,
        val with: String
    )

    annotation class Narrows(vararg val value: Narrow)


}
