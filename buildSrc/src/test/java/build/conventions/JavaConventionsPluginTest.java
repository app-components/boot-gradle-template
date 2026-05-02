package build.conventions;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaConventionsPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void failsTestTaskWhenJUnitAssertionImportsAreUsed() throws IOException {
        writeBuildFiles();
        writeFile(
                projectDir.resolve("sample/src/test/java/com/example/SampleTest.java"),
                """
                        package com.example;

                        import org.junit.jupiter.api.Test;
                        import static org.junit.jupiter.api.Assertions.assertTrue;

                        class SampleTest {

                          @Test
                          void passes() {
                            assertTrue(true);
                          }
                        }
                        """
        );

        BuildResult result = runner(":sample:test").buildAndFail();

        assertTrue(result.getOutput().contains("> Task :sample:test"));
        assertTrue(result.getOutput().contains("> Task :sample:defaultRestrictImports FAILED"));
        assertTrue(result.getOutput().contains("Use assertj instead"));
        assertTrue(result.getOutput().contains("org.junit.jupiter.api.Assertions"));
    }

    @Test
    void allowsAssertJAssertions() throws IOException {
        writeBuildFiles();
        writeFile(
                projectDir.resolve("sample/src/test/java/com/example/SampleTest.java"),
                """
                        package com.example;

                        import org.junit.jupiter.api.Test;
                        import static org.assertj.core.api.Assertions.assertThat;

                        class SampleTest {

                          @Test
                          void passes() {
                            assertThat(true).isTrue();
                          }
                        }
                        """
        );

        BuildResult result = runner(":sample:test").build();

        assertTrue(result.getOutput().contains("> Task :sample:test"));
        assertTrue(result.getOutput().contains("> Task :sample:defaultRestrictImports"));
    }

    private GradleRunner runner(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(arguments)
                .withPluginClasspath();
    }

    private void writeBuildFiles() throws IOException {
        writeFile(
                projectDir.resolve("settings.gradle.kts"),
                """
                        rootProject.name = "test-project"

                        include("platform")
                        include("sample")
                        """
        );
        writeFile(
                projectDir.resolve("build.gradle.kts"),
                """
                        subprojects {
                            repositories {
                                mavenCentral()
                            }
                        }
                        """
        );
        writeFile(
                projectDir.resolve("platform/build.gradle.kts"),
                """
                        plugins {
                            `java-platform`
                        }
                        """
        );
        writeFile(
                projectDir.resolve("sample/build.gradle.kts"),
                """
                        plugins {
                            id("java.conventions")
                        }

                        dependencies {
                            testImplementation("org.assertj:assertj-core:3.27.3")
                        }

                        tasks.named("generateGitProperties") {
                            enabled = false
                        }
                        """
        );
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
