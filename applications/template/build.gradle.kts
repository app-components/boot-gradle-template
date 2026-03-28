plugins {
    id("java.conventions")
    alias(libs.plugins.boot)
}

dependencies {
    implementation(libs.boot.starter.webmvc)
    implementation(libs.boot.starter.actuator)
    implementation(libs.boot.starter.data.jpa)
    implementation(libs.boot.starter.flyway)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.boot.starter.data.jpa.test)
    testImplementation(libs.boot.starter.webmvc.test)
    testImplementation(libs.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}
