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
import java.util.concurrent.ConcurrentLinkedQueue


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
@ImplementModel(classKind = Kind.OPEN)
interface TimeSettingTemplate
//    : WithAnyId

interface WithTimeSettings {
    val timeSettings: List<TimeSettingTemplate>
}

/**
 * Part of [Schedule.events]
 */
@ImplementModel
@ImplementComparable(order = ["id", "endTime"])
interface EventTemplate :
    WithUUId,
    WithTimeSettings,
    WithStartEndTimeStamp {

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
interface Schedule:
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
@ImplementModel(classKind = Kind.OPEN)
@ClassInitializers(list = [ClassMapping(source = MutableList::class, target = LinkedList::class)])
interface PeriodicalScheduleTemplate :
        Schedule,
        WithPeriod

fun main() {

    println(ConcurrentLinkedQueue(
    "kotlin.collections.List<net.eraga.tools.models.examples.TimeSettingTemplate, kotlin.Long>"
        .split("[\\<\\>\\,]".toRegex())
        .filter { it.isNotBlank() }
    ).peek())
//        .forEach {
//            println("match: '${it.trim()}'")
//        }

    val event = Event()
    val event2 = Event()
    if(event > event2) {
        print("can't be")
    }
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
