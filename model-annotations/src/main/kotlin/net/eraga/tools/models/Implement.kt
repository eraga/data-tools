package net.eraga.tools.models

import net.eraga.tools.models.implement.*
import java.lang.annotation.ElementType

/**
 * **Implement**
 *
 * TODO: Implement description
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
     *
     */
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Init(
            val with: String,
            vararg val scopes: InitScope = []
    )

    enum class InitScope {
        PROPERTY, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR
    }
}
