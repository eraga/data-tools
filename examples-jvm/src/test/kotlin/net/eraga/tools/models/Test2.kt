package net.eraga.tools.models

import net.eraga.commons.Copiable
import net.eraga.tools.models.Implement.*
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import kotlin.Comparable

///**
// * **Test2**
// *
// * TODO: Test2 description
// *
// * @author
// *  [Klaus Schwartz](mailto:klaus@eraga.net)
// *
// *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
// *
// *  Date: 18/07/2021
// *  Time: 21:13
// */
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

@Immutable
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
    Copiable,
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
    interface UpdateIdNameRequest : WithIdAndName {
        @Omit
        @Init("42")
        override val id: Int

        @Init("\"42\"")
        override val name: String

        @Init("\"124\"")
        val propNotKnownToImmutable: Any
    }
}


//@Implement.Immutable("")
//interface PersonJPAModel : PersonModel {
//    override var id: Int
//    override val name: String
//    override val secondName: String
//
//    val person: PersonModel
//
//    @DTO(propsForceNull = true)
//    interface CreateRequest : PersonJPAModel {
//        val something: PersonModel.UpdateIdNameRequest
//    }
//}
//
//data class Test2(
//    val a: String,
//    val b: String
//)

//@Immutable
//@DTO
//interface IntModel: WithIntID
//
interface SomeGeneric<T, E> {
    val genericParam: T
    val otherGeneric: E
    val andOneMoreGeneric: E
//    val listWithGenerics: List<E>
//    val mapWithGenerics: Map<T, E>
}

@Immutable
interface GenericModel: SomeGeneric<Long, Map<String, Long>> {
    @NoInit
    override val otherGeneric: Map<String, Long>
}

@Implement.Immutable
abstract class MeModel() : WithIdAndName {
    final val type = "something"

    @Implement.NoInit
    abstract override val id: Int

    @Implement.NoInit
    abstract var a: String

    constructor(
        id: Int,
        name: String,
        a: String
    ) : this()

    val mapToSmth: String
        get() = "get: $a"
}

fun main() {
    println("done")

    val person = Person()
    val immutable = Person(person).clone()
    val immutablePerson = immutable.toPersonUpdateDTO()
}
