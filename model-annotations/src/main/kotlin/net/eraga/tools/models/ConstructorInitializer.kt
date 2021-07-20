package net.eraga.tools.models

/**
 * Provides default value initializer of property in default constructor
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class ConstructorInitializer(
    val value: String
)
