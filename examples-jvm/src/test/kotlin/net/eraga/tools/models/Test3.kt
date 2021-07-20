package net.eraga.tools.models

import java.time.LocalDateTime
import javax.persistence.*

/**
 * **Test3**
 *
 * TODO: Class Test3 description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 19/07/2021
 *  Time: 14:38
 */
interface WithEnabled {
    val enabled: Boolean
}

interface WithCreatedUpdatedDateTime {
    val created: LocalDateTime
    val updated: LocalDateTime
}

interface UserModel : WithLongId, WithCreatedUpdatedDateTime, WithEnabled {
    val email: String
    val role: UserRole
    val password: String
}

@Implement.DTO
@Entity
@Table(name = "users")
data class User(
        @ConstructorInitializer("\"nowByUTC()\"")
        override var email: String,

        @Implement.Omit("UserDTO")
        override var password: String,

        @Column(nullable = false)
        @Enumerated(EnumType.STRING)
        override var role: UserRole = UserRole.ROLE_USER
) : UserModel {
    @ConstructorInitializer("42")
    override var id: Long = 0

    @ConstructorInitializer("LocalDateTime.now()")
    @Column(nullable = false)
    override var created: LocalDateTime = LocalDateTime.now()

    @ConstructorInitializer("LocalDateTime.now()")
    @Column(nullable = false)
    override var updated: LocalDateTime = LocalDateTime.now()

    @Column(nullable = false)
    override var enabled: Boolean = true
}

enum class UserRole {
    ROLE_ADMIN,
    ROLE_USER
}

fun main() {

}
