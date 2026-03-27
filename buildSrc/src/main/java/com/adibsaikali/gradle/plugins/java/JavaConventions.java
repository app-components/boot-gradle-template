package com.adibsaikali.gradle.plugins.java;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.JvmTestSuitePlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.testing.base.TestingExtension;

import java.util.List;

class JavaConventions {

    private final int JAVA_VERSION = 25;
    private final List<String> compilerArgs = List.of(
            "-parameters",
            "-Werror",
            "-Xlint:unchecked",
            "-Xlint:deprecation",
            "-Xlint:rawtypes",
            "-Xlint:varargs");

    public void apply(Project project) {
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        project.getPluginManager().apply(JvmTestSuitePlugin.class);
        applyCompilerConventions(project);
        applyJarConventions(project);
        applyTestConventions(project);
        applyAotCompileConventions(project);
        applyPlatforms(project);
    }

    /**
     * Applies shared platform dependencies across different dependency configurations.
     * This ensures consistent versions are used throughout the project by referencing a common platform.
     * Also applies additional platform dependencies when optional plugins like
     * {@code java-test-fixtures} or {@code org.springframework.boot} are present.
     */
    private void applyPlatforms(Project project) {
        DependencyHandler dependencies = project.getDependencies();
        Dependency platform = dependencies.platform(project.project(":platform"));

        dependencies.add("implementation", platform);
        dependencies.add("annotationProcessor", platform);
        dependencies.add("testImplementation", platform);

        project.getPluginManager().withPlugin("java-test-fixtures", plugin ->
                dependencies.add("testFixturesImplementation", platform)
        );
        project.getPluginManager().withPlugin("org.springframework.boot", plugin ->
                dependencies.add("developmentOnly", platform)
        );
    }

    private void applyCompilerConventions(Project project) {
        // set a Java tool chain version
        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(JAVA_VERSION));

        // set compiler flags
        project.getTasks().withType(JavaCompile.class, (compile) -> {
            compile.getOptions().setEncoding("UTF-8");
            List<String> args = compile.getOptions().getCompilerArgs();
            args.addAll(compilerArgs);
        });
    }

    /**
     * Applies conventions to the JAR task:
     * <ul>
     *     <li>If the Spring Boot plugin is applied, disables the default {@code jar} task.
     *         Spring Boot uses {@code bootJar} to generate a runnable fat JAR, and the plain
     *         {@code jar} is unnecessary and can cause confusion or unintentional Docker build issues.</li>
     *     <li>For all {@code Jar} tasks, sets the archive base name to match the project's Gradle path
     *         (e.g., {@code :my-service} becomes {@code my-service.jar}). This provides consistent
     *         artifact names across a multi-module build.</li>
     * </ul>
     */
    private void applyJarConventions(Project project) {
        project.getPluginManager().withPlugin("org.springframework.boot", plugin ->
            project.getTasks().named("jar", Jar.class).configure(jar -> jar.setEnabled(false))
        );

        project.getTasks().withType(Jar.class).configureEach(jar ->
                jar.getArchiveBaseName().set(project.getPath().replace(":", "-").substring(1))
        );
    }

    /**
     * Applies shared platform dependencies across different dependency configurations.
     * This ensures consistent versions are used throughout the project by referencing a common platform.
     * Also applies additional platform dependencies when optional plugins like
     * {@code java-test-fixtures} or {@code org.springframework.boot} are present.
     */
    private void applyTestConventions(Project project) {

        var testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().withType(JvmTestSuite.class, c -> c.useJUnitJupiter());


        project.getTasks().withType(Test.class).forEach(test -> {
            test.testLogging(loggingContainer -> {
                loggingContainer.setShowStandardStreams(false);
                loggingContainer.setShowCauses(true);
                loggingContainer.setShowStackTraces(true);
                loggingContainer.setExceptionFormat(TestExceptionFormat.FULL);
            });
        });
    }

    /**
     * Configures the compileAotJava task when the Spring AOT plugin is applied.
     * Adds specific lint options and suppresses warnings.
     */
    private void applyAotCompileConventions(Project project) {
        project.getPluginManager().withPlugin("org.springframework.boot.aot", plugin ->
            project.getTasks().named("compileAotJava", JavaCompile.class).configure(task -> {
                task.getOptions().getCompilerArgs().addAll(List.of(
                    "-Xlint:unchecked",
                    "-Xlint:deprecation"
                ));
                task.getOptions().setWarnings(false);
            })
        );
    }
}
