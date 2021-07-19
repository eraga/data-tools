package net.eraga.tools.models.implement

import net.eraga.tools.models.Implement

/**
 * Grouping for APT
 */
annotation class JPAEntities(
        vararg val value: Implement.JPAEntity
)
