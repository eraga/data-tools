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

import net.eraga.tools.models.ClassInitializers
import net.eraga.tools.models.ClassMapping
import net.eraga.tools.models.ImplementModel
import net.eraga.tools.models.Kind
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
interface WithLongId {
    /**
     * Уникальный идентификатор
     */
    val id: Long
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
        WithLongId,
        WithTimeSettings

interface WithEvents {
    /**
     * Коллекция объявлений
     */
//    val events: MutableList<EventModel>
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
    println(mapOf(
            Pair(MutableSet::class, HashSet::class),
            Pair(MutableMap::class, HashMap::class),
            Pair(MutableIterable::class, ArrayList::class),
            Pair(MutableCollection::class, ArrayList::class),
            Pair(MutableList::class, ArrayList::class)
    ))
}
