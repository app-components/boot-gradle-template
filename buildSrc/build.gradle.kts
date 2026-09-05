plugins {
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.10.2" )
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties:4.0.1")
    implementation("de.skuzzle.restrictimports:restrict-imports-gradle-plugin:3.0.1")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.54.0")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
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
