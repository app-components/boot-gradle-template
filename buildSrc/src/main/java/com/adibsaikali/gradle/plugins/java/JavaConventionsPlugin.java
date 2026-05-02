package com.adibsaikali.gradle.plugins.java;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.diffplug.spotless.LineEnding;
import com.gorylenko.GitPropertiesPlugin;
import com.gorylenko.GitPropertiesPluginExtension;
import de.skuzzle.restrictimports.gradle.RestrictImports;
import de.skuzzle.restrictimports.gradle.RestrictImportsPlugin;
import org.gradle.api.Plugin;
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
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.testing.base.TestingExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class JavaConventionsPlugin implements Plugin<Project> {

    // Update this version when the shared Java toolchain for the build changes.
    private static final int JAVA_VERSION = 25;

    // Update these flags when the shared compiler policy for the build changes.
    private static final List<String> COMPILER_ARGS = List.of(
            "-parameters",        // Retain method parameter names in compiled bytecode for reflection.
            "-Werror",            // Fail the build when the compiler emits any warning.
            "-Xlint:unchecked",   // Warn about unchecked generic operations and conversions.
            "-Xlint:deprecation", // Warn when code uses deprecated APIs.
            "-Xlint:removal",     // Warn when code uses APIs marked for removal in a future JDK release.
            "-Xlint:rawtypes",    // Warn when code uses raw types instead of generics.
            "-Xlint:overrides",   // Warn about suspicious or inconsistent method override declarations.
            "-Xlint:this-escape", // Warn when constructors leak this before subclass initialization completes.
            "-Xlint:try",         // Warn about issues in try-with-resources and exception handling structure.
            "-Xlint:varargs");    // Warn about potentially unsafe varargs usage.

    // Update this header when the shared Java file license text changes.
    private static final String JAVA_LICENSE_HEADER = """
            /*
             * Copyright $YEAR Programming Mastery Inc.
             *
             * All Rights Reserved Unauthorized copying of this file, via any medium is strictly prohibited.
             *
             * Proprietary and confidential
             */
            """;

    @Override
    public void apply(Project project) {
        var pluginManager = project.getPluginManager();

        // Apply the Gradle plugins that the shared Java conventions depend on.
        pluginManager.apply(JavaLibraryPlugin.class);
        pluginManager.apply(JvmTestSuitePlugin.class);
        pluginManager.apply(RestrictImportsPlugin.class);
        pluginManager.apply(SpotlessPlugin.class);
        pluginManager.apply(GitPropertiesPlugin.class);

        // Configure the shared Java project policies that each module inherits.
        configureJavaCompilation(project);
        banJunitAssertions(project);
        enforceFormattingStandards(project);
        configureTests(project);
        publishGitMetadata(project);

        // Add conventions that only matter when optional plugins or dependencies are present.
        configureSpringConventions(project);
        addPlatformDependencies(project);
    }

    private void configureJavaCompilation(Project project) {
        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(JAVA_VERSION));

        project.getTasks().withType(JavaCompile.class, compile -> {
            compile.getOptions().getRelease().set(JAVA_VERSION);
            compile.getOptions().setEncoding("UTF-8");
            List<String> args = compile.getOptions().getCompilerArgs();
            args.addAll(COMPILER_ARGS);
        });
    }

    private void banJunitAssertions(Project project) {
        // Define the restrict-imports rule: ban JUnit assertion imports everywhere and explain why.
        project.getTasks().withType(RestrictImports.class).configureEach(task ->
                task.group(group -> {
                    group.getReason().set("Use assertj instead");
                    group.getBasePackages().set(List.of("**"));
                    group.getBannedImports().set(List.of(
                            "org.junit.jupiter.api.Assertions",
                            "static org.junit.jupiter.api.Assertions.*",
                            "static org.junit.Assert.*",
                            "org.junit.Assert"
                    ));
                })
        );

        // Run the rule after every test task so the check is part of normal test execution.
        project.getTasks().withType(Test.class).configureEach(test ->
                test.finalizedBy(project.getTasks().withType(RestrictImports.class))
        );
    }

    private void enforceFormattingStandards(Project project) {
        SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);

        // Apply shared Spotless defaults across all configured formats.
        spotless.setEncoding(StandardCharsets.UTF_8);
        spotless.setLineEndings(LineEnding.UNIX);
        spotless.setEnforceCheck(true);

        // Format Java sources with google-java-format and the shared license header.
        spotless.java(java -> {
            java.googleJavaFormat();
            java.removeUnusedImports();
            java.licenseHeader(JAVA_LICENSE_HEADER);
            java.targetExclude("build/generated/**");
        });

        // Format SQL sources with the shared DBeaver configuration.
        spotless.sql(sql -> {
            sql.target("**/*.sql");
            sql.dbeaver().configFile(project.getRootProject().file("buildSrc/spotless/dbeaver.properties"));
        });

        project.getLogger().info("Spotless conventions configured");
    }

    /**
     * Configures JUnit Jupiter as the default test suite and standardizes test logging.
     */
    private void configureTests(Project project) {
        var testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().withType(JvmTestSuite.class, suite -> suite.useJUnitJupiter());

        project.getTasks().withType(Test.class).configureEach(test ->
                test.testLogging(loggingContainer -> {
                    loggingContainer.setShowStandardStreams(false);
                    loggingContainer.setShowCauses(true);
                    loggingContainer.setShowStackTraces(true);
                    loggingContainer.setExceptionFormat(TestExceptionFormat.FULL);
                })
        );
    }

    private void publishGitMetadata(Project project) {
        GitPropertiesPluginExtension git = project.getExtensions().getByType(GitPropertiesPluginExtension.class);
        git.setKeys(
                List.of(
                        "git.branch",
                        "git.build.version",
                        "git.closest.tag.commit.count",
                        "git.closest.tag.name",
                        "git.commit.id",
                        "git.commit.id.abbrev",
                        "git.commit.id.describe",
                        "git.commit.message.full",
                        "git.commit.message.short",
                        "git.commit.time",
                        "git.commit.user.email",
                        "git.commit.user.name",
                        "git.dirty",
                        "git.remote.origin.url",
                        "git.tags"
                )
        );

        git.setFailOnNoGitDirectory(true);
        git.getDotGitDirectory().set(project.getRootProject().getLayout().getProjectDirectory().dir(".git"));
        project.getLogger().info("GitProperties Plugin configured");
    }

    private void configureSpringConventions(Project project) {
        project.getPluginManager().withPlugin("org.springframework.boot", plugin -> {
            project.getTasks().named("jar").configure(task -> task.setEnabled(false));
            project.getDependencies().add("developmentOnly", project.getDependencies().platform(project.project(":platform")));
        });

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

    /**
     * Applies shared platform dependencies across the main dependency configurations and
     * test fixtures when that plugin is present.
     */
    private void addPlatformDependencies(Project project) {
        DependencyHandler dependencies = project.getDependencies();
        Dependency platform = dependencies.platform(project.project(":platform"));

        dependencies.add("implementation", platform);
        dependencies.add("annotationProcessor", platform);
        dependencies.add("testImplementation", platform);

        project.getPluginManager().withPlugin("java-test-fixtures", plugin ->
                dependencies.add("testFixturesImplementation", platform)
        );
    }
}
