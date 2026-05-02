plugins {
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "com.programmingmastery.conventions"
version = "1.0.0"

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0" )
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:2.5.7")
    implementation("de.skuzzle.restrictimports:restrict-imports-gradle-plugin:3.0.0")
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {

    plugins.create("java.conventions") {
        id = name
        implementationClass = "com.adibsaikali.gradle.plugins.java.JavaConventionsPlugin"
    }

    plugins.create("maven.publish.conventions") {
        id = name
        implementationClass = "com.adibsaikali.gradle.plugins.maven.MavenConventionsPlugin"
    }
}
