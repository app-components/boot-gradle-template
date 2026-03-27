plugins {
    id("java.conventions")
    alias(libs.plugins.boot)
}

dependencies {
    implementation(libs.boot.starter.webmvc)
    implementation(libs.boot.starter.actuator)
}
