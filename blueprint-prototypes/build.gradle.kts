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
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
    jvmToolchain(21)
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
