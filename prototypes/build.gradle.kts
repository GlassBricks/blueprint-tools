plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":blueprint"))
    testImplementation(kotlin("test"))
    testImplementation(project(":test-util"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
}

val generatedDir = layout.buildDirectory.dir("generated").get()
sourceSets {
    main {
        kotlin.srcDir(generatedDir)
    }
}

tasks.register<JavaExec>("prototypeCodegen") {
    mainClass = "glassbricks.factorio.MainKt"
    classpath = project("codegen").sourceSets["main"].runtimeClasspath
    args = listOf(generatedDir.asFile.absolutePath)
    outputs.dir(generatedDir)
}

tasks.compileKotlin {
    dependsOn("prototypeCodegen")
}
