plugins {
    id("java.conventions")
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation(libs.threeten.extra)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.threeten.extra)
}
