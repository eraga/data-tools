package net.eraga.tools.models

/**
 * Provides default value initializer of property in default constructor
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ConstructorInitializer(
    val value: String
)
