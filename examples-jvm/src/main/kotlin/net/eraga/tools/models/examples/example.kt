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
interface TimeSettingModel

interface WithTimeSettings {
    val timeSettings: List<TimeSettingModel>
}

/**
 * Part of [Schedule.events]
 */
@ImplementModel
interface EventModel :
    WithUUId,
    WithTimeSettings,
    WithStartEndTimeStamp,
    Comparable<EventModel> {

    @PreventOverride
    val someUtilityCalc: Long
    get() = startTime + endTime

    override fun compareTo(other: EventModel): Int {
        throw UnsupportedOperationException("not implemented") //TODO
    }
}

interface WithEvents {
    /**
     * Коллекция объявлений
     */
    val events: MutableList<EventModel>
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
interface PeriodicalScheduleModel :
        Schedule,
        WithPeriod

fun main() {
    val event = Event()
    val event2 = Event()
    if(event > event2) {
        print("can't be")
    }

    println(mapOf(
            Pair(MutableSet::class, HashSet::class),
            Pair(MutableMap::class, HashMap::class),
            Pair(MutableIterable::class, ArrayList::class),
            Pair(MutableCollection::class, ArrayList::class),
            Pair(MutableList::class, ArrayList::class)
    ))
}
