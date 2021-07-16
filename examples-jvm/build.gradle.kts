plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
    id("lt.petuska.npm.publish") version "1.1.2"

}

version = "1.0.2-SNAPSHOT"



repositories {
    mavenCentral()
}

val typeScriptGenerated = "$buildDir/generated/source/kaptTypeScript/main"
//val typeScriptGenerated = "$buildDir/publications/npm/${project.name}"

//kapt {
//    arguments {
//        arg("kapt.typescript.generated", typeScriptGenerated)
//    }
//}

//kotlin {
//    js(IR) {
//        binaries.library()
//        browser() // or nodejs()
//    }
//}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.eclipse.persistence:javax.persistence:2.2.1")

    compileOnly(project(":model-annotations"))
    kapt(project (":model-processor"))


    testCompileOnly(project(":model-annotations"))
    kaptTest(project (":model-processor"))
    testImplementation(group = "junit", name = "junit", version = "4.12")
}

//compileKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}
//compileTestKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}

kotlin {
//    org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm {
//
//    }
//    target {
//
//    }
    //sourceCompatibility = 1.8
//targetCompatibility = 1.8
}

npmPublishing {
    readme = file("README.MD") // (optional) Default readme file
    organization = "my.org" // (Optional) Used as default scope for all publications
    access = PUBLIC // or RESTRICTED. Specifies package visibility, defaults to PUBLIC
    /*
      Enables kotlin jar dependencies (including their transitive dependencies) to be resolved bundled automatically for autogenerated publications.
      Defaults to true and can be overridden for each publication.

      Is disabled for IR binaries as they already come with all kotlin dependencies bundled into js output file
     */
    bundleKotlinDependencies = false
    /*
      Adds all bundled dependencies to npm-shrinkwrap.json. Defaults to true and can be overridden for each publication.
      Does not generate a file, even if enabled if there are no bundledDependencies resolved
     */
    shrinkwrapBundledDependencies = true
    /*
      (Optional) Enables run npm publishing with `--dry-run` (does everything except uploading the files). Defaults to false.
     */
    dry = false
    /*
      Overriding default version. Defaults to project.version or rootProject.version, whichever found first
     */
//    version = "1.0.0"

    repositories {
        repository("eraga-unstable") {
            registry = uri("https://packages.eraga.net/repository/eraga-public-npm-unstable") // Registry to publish to
            authToken = "${project.ext["npmToken"]}" // NPM registry authentication token
//            otp = "gfahsdjglknamsdkpjnmasdl" // NPM registry authentication OTP
        }
    }
    publications {
        publication(project.name) { //Custom publication
            bundleKotlinDependencies = false // Overrides the global default for this publication
            shrinkwrapBundledDependencies = false // Overrides the global default for this publication
            nodeJsDir = file("/usr/local") // NodeJs home directory. Defaults to $NODE_HOME if present or kotlinNodeJsSetup output for default publications
//            moduleName = "${project.name}" // Defaults to project name
//            scope = "${project.group}" // Defaults to global organisation
//            readme = file("docs/README.adoc") // Defaults to global readme
//            destinationDir = file(typeScriptGenerated) // Package collection directory, defaults to File($buildDir/publications/npm/$name")
//            main = "my-module-name-override-js.js" // Main output file name, set automatically for default publications
//            types = "types.d.ts" // TS types output file name, set automatically for default publications


            files { _ ->
                from("$typeScriptGenerated")
            }
            // Entirely Optional
            packageJson { // Will be patched on top of default generated package.json
                private = false

                keywords = jsonArray(
                        "kotlin"
                )

                files = fileTree(typeScriptGenerated)
                        .filter { it.isFile }
//                        .filter { it.name != "package.json" }
                        .map { it.name }
                        .toMutableList()

                println("dir ${fileTree(typeScriptGenerated).filter { it.isFile }.map{it.name}}")

                publishConfig {
                    tag = "latest"
                }

                "customField" to jsonObject {
                    "customValues" to jsonArray(1, 2, 3)
                }
            }
        }
    }
}
