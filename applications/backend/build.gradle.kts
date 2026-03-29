plugins {
    id("java.conventions")
    alias(libs.plugins.boot)
}

dependencies {
    implementation(libs.boot.starter.webmvc)
    implementation(libs.boot.starter.actuator)
    implementation(libs.boot.starter.data.jpa)
    implementation(libs.boot.starter.flyway)

    developmentOnly(libs.boot.devtools)
    developmentOnly(libs.boot.docker.compose)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.boot.starter.test)
    testImplementation(libs.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}
