package net.eraga.tools.models

import java.io.Serializable
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
interface WithIntID: WithAnyID {
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
@Implement.DTO
@Implement.DTO(suffix = "Update", propsForceNull = true)
interface PersonModel :
        WithIntID,
        WithName,
        WithSecondName,
        WithIdAndName,
        Comparable<PersonModel>,
        Cloneable,
        Serializable
{
    @ConstructorInitializer("10")
    override val id: Int


    @Implement.DTO
    private interface UpdateIdNameRequest : WithIdAndName

//    fun deepClone(): PersonModel
}

class A(override val id: Int, override val name: String, override val secondName: String):PersonModel {

    override fun compareTo(other: PersonModel): Int {
        clone()
        throw UnsupportedOperationException("not implemented") //TODO
    }

    public override fun clone(): A {
        return super.clone() as A
    }

//    override fun deepClone(): A {
//        val copy = super.clone() as A
//        return copy
//    }
}

@Implement.JPAEntity
interface PersonJPAModel : PersonModel {
    @get:Id
    val `$id`: Int?
    override val id: Int
        get() = `$id`!!
    override val name: String
    override val secondName: String
}

fun main() {
    println("done")
    val a = A(0,"0,0", "")
    a.clone()

    val person = PersonDTO()
}
