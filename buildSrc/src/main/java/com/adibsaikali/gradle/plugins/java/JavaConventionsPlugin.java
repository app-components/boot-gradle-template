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
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.testing.base.TestingExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class JavaConventionsPlugin implements Plugin<Project> {

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
        enforceFormattingStandards(project);
        useJUnitJupiter(project);
        banJunitAssertions(project);
        addGitPropertiesToJar(project);
        usePlatformBom(project);
        disablePlainJarInBootApps(project);

    }

    private void configureJavaCompilation(Project project) {
        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        // Use a consistent JDK toolchain so compilation does not depend on the machine running Gradle.
        java.getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(JAVA_VERSION));

        project.getTasks().withType(JavaCompile.class, compile -> {
            // Compile against the configured Java release to align language level, bytecode target, and JDK APIs.
            compile.getOptions().getRelease().set(JAVA_VERSION);
            // Read Java source files as UTF-8 instead of relying on the platform default charset.
            compile.getOptions().setEncoding("UTF-8");
            // Append the shared compiler policy without discarding any arguments already added elsewhere.
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

    private void useJUnitJupiter(Project project) {
        var testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().withType(JvmTestSuite.class, suite -> suite.useJUnitJupiter());
    }

    private void addGitPropertiesToJar(Project project) {
        GitPropertiesPluginExtension git = project.getExtensions().getByType(GitPropertiesPluginExtension.class);
        git.setKeys(
                List.of(
                        "git.remote.origin.url",  // Which repo the jar was built from.
                        "git.commit.id",          // Exact commit SHA — the durable, unambiguous identity of the source state.
                        "git.commit.id.describe", // Human-readable handle (e.g. v1.2.3-4-gabcdef) so a release can be identified without cloning.
                        "git.dirty"               // Whether the working tree had uncommitted changes — false means the jar matches the SHA exactly.
                )
        );

        git.setFailOnNoGitDirectory(true);
        git.getDotGitDirectory().set(project.getRootProject().getLayout().getProjectDirectory().dir(".git"));
        project.getLogger().info("GitProperties Plugin configured");
    }

    private void disablePlainJarInBootApps(Project project) {
        project.getPluginManager().withPlugin("org.springframework.boot", plugin ->
                project.getTasks().named("jar").configure(task -> task.setEnabled(false))
        );
    }

    private void usePlatformBom(Project project) {
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
}
