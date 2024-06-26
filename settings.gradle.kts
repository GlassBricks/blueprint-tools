plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "factorio-tools"

include("prototypes")
include("prototypes:codegen")
include("blueprint")
include("blueprint-model")
