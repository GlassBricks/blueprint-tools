plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "factorio-tools"

include("blueprint")
include("prototypes")
include("model")
include("pole-optimizer")
include("scripts")
