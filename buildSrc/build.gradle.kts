plugins {
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
}

group = "com.programmingmastery.conventions"
version = "1.0.0"

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0" )
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:2.5.7")
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
