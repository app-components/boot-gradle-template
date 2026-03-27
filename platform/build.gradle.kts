plugins {
    `java-platform`
    id("maven.publish.conventions")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.boot.bom))
    api(platform(libs.cloud.bom))

    constraints {
        // testing dependencies
        api(libs.greenmail)
        api(libs.equals.verifier)
        api(libs.threeten.extra)
    }
}
