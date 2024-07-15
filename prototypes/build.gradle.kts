plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

val generateDataRunSrcDir = layout.buildDirectory.dir("generateDataRunSrc").get()

val generatedDir = layout.buildDirectory.dir("generated").get()
val generatedSrcDir = generatedDir.dir("kotlin")
val generatedResourcesDir = generatedDir.dir("resources")

sourceSets {
    main {
        kotlin.srcDir(generatedSrcDir)
        resources.srcDir(generatedResourcesDir)
    }

    val codegen by creating

    // slightly hacky way to get script which depends on classes but not resources
    // of the main source set, where the IDE is also happy about it
    val generateDataRaw by creating {
        compileClasspath += main.get().compileClasspath + main.get().output
    }

    val generateDataRawRun by creating {
        kotlin.setSrcDirs(listOf(generateDataRunSrcDir.dir("kotlin")))
        resources.setSrcDirs(listOf(generateDataRunSrcDir.dir("resources")))
        // depend on classes only, but not resources
        compileClasspath += main.get().compileClasspath + main.get().output.classesDirs
        runtimeClasspath += compileClasspath
    }
}

dependencies {
    api(project(":blueprint"))
    testImplementation(kotlin("test"))

    "codegenImplementation"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    "codegenImplementation"("com.squareup:kotlinpoet:1.17.0")
}


kotlin {
    explicitApi()
}
tasks {
    test {
        useJUnitPlatform()
    }

    val copyGenerateDataRawSrc by registering(Copy::class) {
        from("src/generateDataRaw")
        into(generateDataRunSrcDir)
    }

    "compileGenerateDataRawRunKotlin" { dependsOn(copyGenerateDataRawSrc) }
    "processGenerateDataRawRunResources" { dependsOn(copyGenerateDataRawSrc) }

    val generateVanillaDataRaw by registering(JavaExec::class) {
        group = "generate"
        mainClass = "glassbricks.factorio.generatedataraw.MainKt"
        classpath = sourceSets["generateDataRawRun"].runtimeClasspath

        val outputFile = generatedResourcesDir.file("vanilla-data-raw.json")

        args = listOf(outputFile.asFile.absolutePath)
        outputs.file(outputFile)
    }

    processResources {
        dependsOn(generateVanillaDataRaw)
    }

    val generatePrototypes by registering(JavaExec::class) {
        group = "generate"
        mainClass = "glassbricks.factorio.prototypecodegen.MainKt"
        classpath = sourceSets["codegen"].runtimeClasspath
        args = listOf(generatedSrcDir.asFile.absolutePath)
        outputs.dir(generatedSrcDir)
    }

    compileKotlin {
        dependsOn(generatePrototypes)
    }
}
