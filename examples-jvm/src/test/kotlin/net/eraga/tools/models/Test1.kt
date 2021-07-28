//package net.eraga.tools.models
//
///**
// * **net.eraga.tools.models.Test1**
// *
// * @author
// *  [Klaus Schwartz](mailto:klaus@eraga.net)
// *
// *  Developed at [eRaga InfoSystems](https://www.eraga.net/)
// *
// *  Date: 16/07/2021
// *  Time: 12:08
// */
//interface WithAnyId {
//    val id: Any
//}
//
//interface WithStringId : WithAnyId {
//    override val id: String
//}
//
//interface WithLongId : WithAnyId {
//    override val id: Long
//}
//
//@ImplementationSettings
//@CompareTo(order = [
//    "id",
//    "weight"
//], compareAllProperties = false)
//interface CategoryTemplate
//    : WithLongId {
//    val weight: Int
//    val name: String?
//}
//
//interface WithDayOfWeek {
//    val dayOfWeek: Int
//}
//
//interface WithStartEndRelativeTime {
//    val relativeStartTime: Long
//    val relativeEndTime: Long
//}
////
////@ImplementationSettings(inheritTemplate = false)
////interface TimeSettingTemplate :
////        WithDayOfWeek,
////        WithStartEndRelativeTime
////
////interface PartOfCampaign {
////    val campaignId: Long
////    val weight: Long
////}
////
////interface WithTimeSettings {
////    val timeSettings: List<TimeSettingTemplate>
////}
////
////interface WithDuration {
////    val duration: Long
////}
////
////interface WithStartEndTimeStamp : WithOptionalStartEndTimeStamp {
////    override val startTime: Long
////    override val endTime: Long
////
////    fun timeRange(): LongRange {
////        return LongRange(startTime, endTime - 1)
////    }
////}
////
////interface WithOptionalStartEndTimeStamp {
////    val startTime: Long?
////    val endTime: Long?
////}
////
//////@ImplementModel(immutable = ImplementationMeta(ClassKind.NONE), templateSuffix = "Model")
////@ImplementationTemplates(
////        ImplementationSettings(forceArgNamesInConstructorSkip = 2),
////        ImplementationSettings(
////                inheritTemplate = false,
////                immutable = Details(Kind.NONE),
////                mutable = Details(Kind.NONE),
////                kclass = Details(Kind.DATA,"Dto")
////        ),
////
////)
////@Comparable(order = [
////    "-weight",
////    "duration",
////    "campaignId",
////    "id",
////    "startTime"
////])
////@HashCode
////@Equals
////interface AdvertisementTemplate :
////        WithLongId,
////        PartOfCampaign,
////        WithTimeSettings,
////        WithDuration,
////        WithStartEndTimeStamp {
////    //    @ConstructorInitializer("Category()")
//////    @GeneratedClass("net.eraga.tools.models.CategoryModel")
////    val categoryTemplate: CategoryTemplate
////    val array: Array<Int>
////    val list: MutableList<Int>
////}
////
////@ImplementDTO("DTO")
////@ImplementDTO("XYZ")
////interface MyEntityModel {
////    val id: Int
////}
////
//////@Repeatable
//////@java.lang.annotation.Repeatable(ImplementDTO::class)
//////@Retention(AnnotationRetention.SOURCE)
//////@ImplementTemplate(
//////        immutable = ImplementationMeta(ClassKind.NONE),
//////        mutable = ImplementationMeta(ClassKind.NONE),
//////        templateSuffix = "Model")
//////annotation class ImplementDTO(val name: String = "")
//////interface ScheduleModel<out T : WithAnyId> : WithLongId {
//////    val scheduleItems: List<T>
//////}
//////
//////@ImplementShort
//////interface WeeklyScheduleModel : ScheduleModel<WithLongId>
////
//////@ImplementModel
//////@HashCode
//////@Equals
//////@ImplementComparable
//////@ImplementTemplate(
//////        inheritTemplate = false,
//////        immutable = Details(kind = Kind.NONE),
//////        mutable = Details(kind = Kind.NONE),
//////        kclass = Details(Kind.OPEN_CLASS, "Request"),
//////        templateSuffix = "Model")
//////@ImplementTemplate(
//////        inheritTemplate = false,
//////        immutable = Details(kind = Kind.NONE),
//////        mutable = Details(kind = Kind.NONE),
//////        kclass = Details(Kind.OPEN_CLASS, "Response"),
//////        templateSuffix = "Model")
//////interface SomeNewEntityModel :
//////        WithLongId,
//////        PartOfCampaign,
//////        WithTimeSettings,
//////        WithDuration,
//////        WithStartEndTimeStamp
////
////object Test1 {
////    @JvmStatic
////    fun main(args: Array<String>) {
////        val a: CategoryModel? = null
////        println("done")
////    }
////}
