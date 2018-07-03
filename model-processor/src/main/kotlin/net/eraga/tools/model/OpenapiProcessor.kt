//package net.eraga.openapi3
//
//import com.google.auto.service.AutoService
//import com.sun.tools.javac.code.Symbol
//import javax.annotation.processing.*
//import javax.lang.model.util.Elements
//import javax.lang.model.util.Types
//import javax.tools.Diagnostic
//import kotlin.annotation.AnnotationTarget.*
//import java.util.HashMap
//import javax.lang.model.SourceVersion
//import javax.lang.model.element.*
//import javax.lang.model.type.DeclaredType
//import kotlin.reflect.full.declaredMemberProperties
//import javax.lang.model.element.TypeElement
//import javax.lang.model.util.ElementFilter
//import javax.lang.model.type.TypeMirror
//import javax.lang.model.element.VariableElement
//
//
//
//
///**
// * Date: 21/06/2018
// * Time: 11:45
// */
//
////@Retention(AnnotationRetention.SOURCE)
//@Target(CLASS)
//annotation class OpenApiInterface()
//
//class Test {
//    var string = ""
//}
//
//@AutoService(Processor::class)
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
//class OpenApiProcessor() : AbstractProcessor() {
//
//    private lateinit var messager: Messager
//    private lateinit var typesUtil: Types
//    private lateinit var elementsUtil: Elements
//    private lateinit var filer: Filer
//
//    override fun init(processingEnv: ProcessingEnvironment) {
//        super.init(processingEnv);
//        messager = processingEnv.getMessager()
//        typesUtil = processingEnv.getTypeUtils()
//        elementsUtil = processingEnv.getElementUtils()
//        filer = processingEnv.getFiler()
//    }
//
//    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
//        messager.printMessage(Diagnostic.Kind.WARNING, "================================================")
//        messager.printMessage(Diagnostic.Kind.WARNING, annotations.toString())
//
//        for (element in roundEnv.getElementsAnnotatedWith(OpenApiInterface::class.java)) {
////            val sym = (element as Symbol)
////            if (sym.owner == null) {
////                messager.printMessage(Diagnostic.Kind.ERROR, "Enclosing element should be Class, found nothing")
////                continue
////            }
//////            element.members()
////            printMembers(element)
//
////            val te = findEnclosingTypeElement(element)
////            log("Scanning Type ${te?.qualifiedName}")
//
////            ElementFilter.methodsIn(element.enclosedElements).forEach {
////                log(it.toString() + " ———— " + it.defaultValue)
////
////            }
//
//            element.enclosedElements.forEach {
//                log(it.toString() + ": " + it.kind + " ———— " + it.enclosedElements.size)
//            }
//
//            val fields = ElementFilter.fieldsIn(element.enclosedElements).forEach {
//                val fieldType = it.asType()
//                val fullTypeClassName = fieldType.toString()
//                // Validate fullTypeClassName
//                log(fullTypeClassName)
//            }
//
//
////            if (sym.owner.getKind().isClass) {
////                val enclosure = Class.forName(sym.owner.toString())
////                val instance = enclosure.newInstance()
//
////                val tiddlerClassMembers = instance.javaClass.kotlin.declaredMemberProperties
////                val fieldNames = tiddlerClassMembers.map { it.name }
//
////            } else {
////                messager.printMessage(Diagnostic.Kind.ERROR, "Enclosing element should be Class, found ${sym.owner.getKind()}")
////                continue
////            }
//
//
////            sym.getKind()
//
////            if (element.getKind() != ElementKind.CLASS) {
////                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
////                return true;
////            }
////
////            TypeElement typeElement = (TypeElement) element;
////            activitiesWithPackage.put(
////                    typeElement.getSimpleName().toString(),
////                    elements.getPackageOf(typeElement).getQualifiedName().toString());
//        }
//
//        return false
//    }
//
//    private fun printMembers(element: Symbol, shift: Int = 0) {
//        var prefix = ""
//        for (i in 0..shift) {
//            prefix += "--"
//        }
//
//        log("$prefix " + element.simpleName.toString())
//        element.members()?.elements?.forEach {
//            printMembers(it, shift + 1)
//        }
//    }
//
//    override fun getSupportedAnnotationTypes(): MutableSet<String> {
//        val set = HashSet<String>()
//        set.add(OpenApiInterface::class.qualifiedName!!)
//        return set
//    }
//
//    private fun <T> findAnnotationValue(element: Element, annotationClass: String,
//                                        valueName: String, expectedType: Class<T>): T? {
//        var ret: T? = null
//        for (annotationMirror in element.annotationMirrors) {
//            val annotationType = annotationMirror.annotationType
//            val annotationElement = annotationType
//                    .asElement() as TypeElement
//            if (annotationElement.qualifiedName.contentEquals(
//                            annotationClass)) {
//                ret = extractValue(annotationMirror, valueName, expectedType)
//                break
//            }
//        }
//        return ret
//    }
//
//    private fun <T> extractValue(annotationMirror: AnnotationMirror,
//                                 valueName: String, expectedType: Class<T>): T? {
//        val elementValues = HashMap<ExecutableElement, AnnotationValue>(
//                annotationMirror.elementValues)
//        for ((key, value1) in elementValues) {
//            if (key.simpleName.contentEquals(valueName)) {
//                val value = value1.getValue()
//                return expectedType.cast(value)
//            }
//        }
//        return null
//    }
//
//    fun findEnclosingTypeElement(e: Element): TypeElement? {
//        var elem: Element? = e
//
//        while (elem != null && elem !is TypeElement) {
//            elem = elem.enclosingElement
//        }
//
//        return TypeElement::class.java.cast(e)
//
//    }
//
//
//    fun log(message: String) {
//        messager.printMessage(Diagnostic.Kind.NOTE, message)
//    }
//
//    fun warn(message: String) {
//        messager.printMessage(Diagnostic.Kind.WARNING, message)
//    }
//
//    fun error(message: String) {
//        messager.printMessage(Diagnostic.Kind.ERROR, message)
//    }
//}
