//package net.eraga.openapi3
//
//import com.squareup.kotlinpoet.*
//import java.io.Serializable
//
///**
// * Date: 21/06/2018
// * Time: 11:02
// */
//object InterfaceGenerator {
//    @JvmStatic
//    fun main(args: Array<String>) {
//
////        val handler = Handler::class
//
//
//        val greeterClass = ClassName("com.example.fuck", "Greeter")
//        val file = FileSpec.builder("fuck.it", "HelloWorld")
//                .addType(TypeSpec.classBuilder("FuckIt")
//                        .addModifiers(KModifier.ABSTRACT)
//                        .addSuperinterface(Serializable::class)
//                        .primaryConstructor(FunSpec.constructorBuilder()
//                                .addParameter("name", String::class)
//                                .build())
//                        .addProperty(PropertySpec.builder("name", String::class)
//                                .initializer("name")
//                                .build())
//                        .addFunction(FunSpec.builder("greet")
//                                .addModifiers(KModifier.ABSTRACT)
////                                .returns(ParameterizedTypeName.get(Handler::class, RoutingContext::class))
//
////                                .addStatement("println(%S)", "Hello, \$name")
//                                .build())
//                        .build())
//                .addFunction(FunSpec.builder("main")
//                        .addParameter("args", String::class, KModifier.VARARG)
//                        .addStatement("%T(args[0]).greet()", greeterClass)
//                        .build())
//                .build()
//
//        file.writeTo(System.out)
//    }
//}
