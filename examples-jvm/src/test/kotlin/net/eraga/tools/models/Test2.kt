package net.eraga.tools.models

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import kotlin.Comparable

/**
 * **Test2**
 *
 * TODO: Test2 description
 *
 * @author
 *  [Klaus Schwartz](mailto:klaus@eraga.net)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 18/07/2021
 *  Time: 21:13
 */
interface WithAnyID {
    val id: Any
}

interface WithIntID : WithAnyID {
    override val id: Int
}

interface WithName {
    val name: String
}

interface WithSecondName {
    val secondName: String
}

interface WithIdAndName : WithIntID, WithName

@Implement.Immutable
@Implement.Immutable("", implementAnnotations = "javax.persistence.*")
@Implement.DTO
@Implement.DTO(suffix = "Update", propsForceNull = true)
@Entity
interface PersonModel :
        WithIdAndName,
        WithSecondName,

        Comparable<PersonModel>,
        Cloneable,
        Serializable {

    @get:Id
    @Implement.Init("0")
    @Implement.Init("24", "PersonDTO")
    @Implement.Omit("PersonUpdateDTO")
    override var id: Int

    @Implement.Omit("PersonDTO")
    override val name: String

    @Implement.Omit("PersonDTO")
    val nameOfPerson: String

    @Implement.DTO
    private class UpdateIdNameRequest() : WithIdAndName {
        @Implement.Omit
        @Implement.Init("42")
        override val id: Int = 0

        @Implement.Init("\"42\"")
        override val name: String = ""
    }
}


@Implement.JPAEntity
interface PersonJPAModel : PersonModel {
    override var id: Int
    override val name: String
    override val secondName: String
}

data class Test2(
        val a: String,
        val b: String
)

fun main() {
    println("done")

    val person = Person()
    val immutable = ImmutablePerson(person).clone()
    val immutablePerson = immutable.toPersonUpdateDTO()
}
