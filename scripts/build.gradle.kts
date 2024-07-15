plugins {
    kotlin("jvm")
}

group = "glassbricks.factorio"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(project(":model"))
    testImplementation(project(":pole-optimizer"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
sourceSets {
    main{
        kotlin {
            srcDirs.clear()
        }
    }
    test {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}
