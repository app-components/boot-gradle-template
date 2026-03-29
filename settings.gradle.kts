rootProject.name = "boot-gradle-template"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("platform")
include("components:time")
include("applications:backend")
