plugins {
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0" )
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:2.5.7")
    implementation("de.skuzzle.restrictimports:restrict-imports-gradle-plugin:3.0.0")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {

    plugins.create("java.conventions") {
        id = name
        implementationClass = "build.conventions.JavaConventionsPlugin"
    }

    plugins.create("maven.publish.conventions") {
        id = name
        implementationClass = "build.conventions.MavenConventionsPlugin"
    }
}
