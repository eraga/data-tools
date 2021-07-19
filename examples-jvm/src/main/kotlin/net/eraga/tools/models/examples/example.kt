/**
 * **example**
 *
 * TODO: example description
 *
 * @author
 *  [Klaus Schwartz](mailto:tntclaus@gmail.com)
 *
 *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
 *
 *  Date: 24/09/2018
 *  Time: 11:50
 */
package net.eraga.tools.models.examples

import net.eraga.tools.models.*
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Entity


/**
 * Способ размещения кампании в плейлистах
 */
enum class SomeType {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4
}

/**
 * Свойство наличия уникального идентификатора
 */
interface WithAnyId {
    /**
     * Уникальный идентификатор
     */
    val id: Any
}

interface WithStringId : WithAnyId {
    override val id: String
}

interface WithUUId : WithAnyId {
    @ConstructorInitializer(value = "UUID.randomUUID()")
    override val id: UUID

    @ConstructorInitializer(value = "LocalDateTime.now()")
    val asd: LocalDateTime
}


interface WithLongId : WithAnyId {
    override val id: Long
}

/**
 * Временная настройка, используется в [EventModel.timeSettings]
 */
//@ImplementModel(kclass = ImplementationMeta(ClassKind.OPEN))
interface TimeSettingTemplate
//    : WithAnyId

interface WithTimeSettings {
    val timeSettings: List<TimeSettingTemplate>
}

/**
 * Part of [Schedule.events]
 */
//@ImplementModel
//@ImplementHashCode
//@ImplementEquals
//@ImplementComparable
interface EventTemplate :
        WithUUId,
        WithTimeSettings,
        WithStartEndTimeStamp {

    //    val intArray: IntArray
    val array: Array<Int>
    val longArray: Array<Long>
    val stringArray: Array<String>

    @PreventOverride
    val someUtilityCalc: Long
        get() = startTime + endTime
}

interface WithEvents {
    /**
     * Коллекция объявлений
     */
    val events: MutableList<EventTemplate>
}

/**
 * Базовый интерфейс рекламной кампании
 */
interface Schedule :
        WithLongId,
        WithEvents {

    val type: SomeType

    val isType1: Boolean
        get() = type == SomeType.TYPE1
}

interface WithOptionalStartEndTimeStamp {
    /**
     * Время начала (включительно), UNIX Timestamp, миллисекунды
     */
    val startTime: Long?

    /**
     * Время окончания **(исключительно)**, UNIX Timestamp, миллисекунды
     */
    val endTime: Long?
}


/**
 * Признак начала/окончания по времени UTC
 */
interface WithStartEndTimeStamp : WithOptionalStartEndTimeStamp {
    override val startTime: Long
    override val endTime: Long
}


/**
 * Свойство периодичности
 */
interface WithPeriod {
    /**
     * Продолжительность периода, в СЕКУНДАХ
     */
    val period: Long
}

//@CSModelDSLMarker
//@ImplementModel(kclass = ImplementationMeta(ClassKind.OPEN))
//@ClassInitializers(list = [ClassMapping(source = MutableList::class, target = LinkedList::class)])
interface PeriodicalScheduleTemplate :
        Schedule,
        WithPeriod

data class A(val a: Array<Int> = arrayOf())

//@ImplementModel
//@ImplementEquals
//@ImplementHashCode
//@ImplementComparable
//interface BTemplate {
//    val a: Array<Int>
//}

interface WithAB<A, B>

//@Implementations(
////        ImplementModel(),
//        ImplementModel(
//                kclass = ImplementationMeta(ClassKind.DATA, "Dto"),
//                immutable = ImplementationMeta(ClassKind.NONE, "DtoImmutable"),
//                mutable = ImplementationMeta(ClassKind.NONE, "DtoMutable"),
//                inheritTemplate = false
//        )
//)
//@ImplementComparable
//@ImplementHashCode
//@ImplementEquals
@Entity
interface ShinyObjectTemplate
    : WithUUId, WithAB<WithUUId, Long>
{
//    @ConstructorInitializer("\"Fuck you initialization!\"")
//    var display: String
//    var name: String
//    var gid: String
//    var externalId: Long?
//    var resolution: Pair<Long, Long>?
//    val org: String?
//    val isSomething: Boolean
//    val array: Array<Byte>
//    val stringArray: Array<String?>
//    val longArray: Array<Long>
//    val longList: List<Long?>
//    val list: List<WithUUId>
//    val map: Map<WithAB<Long, Array<Long>>, Long?>
    val map2: Map<Long, Long?>
}

fun main() {
//    println(Array::class.qualifiedName.toString())
//    println(JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(FqName(Array::class.qualifiedName.toString()).toUnsafe())
//        ?.asSingleFqName()
//        ?.asString())
    val a1 = A(arrayOf(1, 2, 3))
    val a2 = A(arrayOf(1, 2, 3))

//    val b1 = B(arrayOf(1, 2, 3))
//    val b2 = B(arrayOf(1, 2, 3))

//    println(a1 == a2)
//    println(b1 == b2)


    val intArray = intArrayOf()
    val intArray2 = intArrayOf()
    intArray.contentEquals(intArray2)

    val a = arrayOf(1, 2, 3)
    val b = arrayOf(1, 2, 3)
    val la = listOf(1, 2, 3)
    val lb = listOf(1, 2, 3)

//    println(a == b)
//    println(a === b)
//    println(a.contentEquals(b))
//    println(a.contentDeepEquals(b))
//
//    println(a.hashCode())
//    println(b.hashCode())
    println(a.contentDeepHashCode())
//    println(b.contentHashCode())
//    println(la.hashCode())
//    println(lb.hashCode())
//
//
//    println(la == lb)
//    println(la === lb)
//    println(la.equals(lb))


//    val event = Event()
//    val event2 = Event()
//    if (event > event2) {
//        print("can't be")
//    }
    val time1 = LocalDateTime.now()
    val time2 = LocalDateTime.now()
    time1.compareTo(time2)
    println(mapOf(
            Pair(MutableSet::class, HashSet::class),
            Pair(MutableMap::class, HashMap::class),
            Pair(MutableIterable::class, ArrayList::class),
            Pair(MutableCollection::class, ArrayList::class),
            Pair(MutableList::class, ArrayList::class)
    ))
}
