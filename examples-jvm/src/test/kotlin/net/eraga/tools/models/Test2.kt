package net.eraga.tools.models

import net.eraga.tools.models.Implement.*
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

@Immutable("")
@DTO
@DTO(suffix = "Update", propsForceNull = true)
@Annotate("Person", Entity::class)
//@GeneratedClass("")
interface PersonModel :
        WithIdAndName,
        WithSecondName,

        Comparable<PersonModel>,
        Cloneable,
        Serializable {

    @Init("0")
    @Init("24", "PersonDTO")
    @Omit("PersonUpdateDTO")
    @Annotate("Person", Id::class)
    @Annotate("Person", Column::class, "nullable = false", "unique = true")
    override var id: Int

    @Omit("PersonDTO")
    override val name: String

    @Omit("PersonDTO")
    val nameOfPerson: String

    val arrayOfPerson: Array<Int>

    @DTO
    private class UpdateIdNameRequest() : WithIdAndName {
        @Omit
        @Init("42")
        override val id: Int = 0

        @Init("\"42\"")
        override val name: String = ""
    }
}


@JPAEntity
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
    val immutable = Person(person).clone()
    val immutablePerson = immutable.toPersonUpdateDTO()
}
