package net.eraga.tools.models.implement

import net.eraga.tools.models.Implement

/**
 * Grouping for APT
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DTOs(vararg val value: Implement.DTO)
