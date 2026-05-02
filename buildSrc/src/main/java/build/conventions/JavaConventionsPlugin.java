package build.conventions;

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
import org.gradle.testing.jacoco.plugins.JacocoPlugin;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JavaConventionsPlugin implements Plugin<Project> {

    // Customize this header to fit your project. The $YEAR token is replaced by Spotless with
    // the file's first-stamp year (preserved across subsequent rewrites). One edit here
    // propagates across the repository on the next spotlessApply.
    private static final String JAVA_LICENSE_HEADER = """
            /*
             * Copyright $YEAR-present the original author or authors.
             */
            """;

    // Update this version when the shared Java toolchain for the build changes.
    private static final int JAVA_VERSION = 25;

    // Minimum percentage of code coverage required; the build fails below this threshold.
    private static final int MINIMUM_COVERAGE_PERCENT = 50;

    // Strict warning policy applied to every module. Modules may remove individual flags from
    // their own build script when a transition (e.g. a major dependency upgrade) temporarily
    // produces too many warnings to fix at once.
    private static final List<String> WARNING_FLAGS = List.of(
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
        pluginManager.apply(JacocoPlugin.class);

        // Configure the shared Java project policies that each module inherits.
        configureJavaCompilationSettings(project);
        enforceFormattingStandards(project);
        useJUnitJupiter(project);
        measureCoverageWithJacoco(project);
        banJunitAssertions(project);
        addGitPropertiesToJar(project);
        addPlatformBomToClasspaths(project);
        disablePlainJarInBootApps(project);

    }

    /**
     * Pins the JDK toolchain, language release, source encoding, and compiler flags so every
     * module compiles identically regardless of the developer's machine. Gradle downloads the
     * configured JDK on demand, so a build never depends on whatever JDK happens to be
     * installed locally.
     *
     * <p>{@code -parameters} is non-negotiable — Spring, Jackson, and similar frameworks read
     * parameter names via reflection at runtime, and removing the flag silently breaks code
     * that relies on parameter binding.
     *
     * <p>The strict warning policy ({@code -Werror} plus the {@code -Xlint} categories in
     * {@link #WARNING_FLAGS}) turns questionable code into build failures rather than warnings
     * ignored in CI logs. Modules can remove individual warning flags from their own build
     * script when a transition (e.g. a major dependency upgrade) makes the strict defaults
     * temporarily painful, without needing to drop the conventions plugin entirely.
     */
    private void configureJavaCompilationSettings(Project project) {
        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        // Use a consistent JDK toolchain so compilation does not depend on the machine running Gradle.
        java.getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(JAVA_VERSION));

        project.getTasks().withType(JavaCompile.class, compile -> {
            // Compile against the configured Java release to align language level, bytecode target, and JDK APIs.
            compile.getOptions().getRelease().set(JAVA_VERSION);
            // Read Java source files as UTF-8 instead of relying on the platform default charset.
            compile.getOptions().setEncoding("UTF-8");
            List<String> args = compile.getOptions().getCompilerArgs();
            // -parameters affects emitted bytecode: Spring, Jackson, and similar frameworks read
            // parameter names via reflection at runtime, so this is non-negotiable.
            args.add("-parameters");
            // Strict warning policy. Modules may remove individual flags downstream if needed.
            args.addAll(WARNING_FLAGS);
        });
    }

    /**
     * Wires up Spotless to format Java and SQL sources to a single shared style — Java with
     * google-java-format and the shared license header, SQL with the shared DBeaver profile.
     * This eliminates style debates and keeps diffs focused on real changes rather than
     * whitespace.
     *
     * <p>Spotless runs as part of the {@code check} task, so any module whose code drifts
     * from the standard style fails the build. Run {@code ./gradlew spotlessApply} (or the
     * {@code s} helper script) to fix violations automatically rather than editing by hand.
     */
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
     * Selects JUnit Jupiter (JUnit 5) as the test framework for every JVM test suite in the
     * module, so every module across the repository runs tests on the same stack and tests
     * written in one module behave identically when copied into another.
     */
    private void useJUnitJupiter(Project project) {
        var testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().withType(JvmTestSuite.class, suite -> suite.useJUnitJupiter());
    }

    /**
     * Wires JaCoCo into every JVM test task in the module. Every test run produces a code
     * coverage report at {@code build/reports/jacoco/} in both HTML (for humans) and XML
     * (for tooling like SonarQube, Codecov, and GitHub coverage actions), and the
     * {@code check} lifecycle enforces a minimum coverage threshold — builds whose coverage
     * falls below {@link #MINIMUM_COVERAGE_PERCENT} fail.
     *
     * <p>The JaCoCo runtime version comes from the Gradle distribution being used, so the
     * tool version moves in lockstep with Gradle upgrades.
     */
    private void measureCoverageWithJacoco(Project project) {
        // Enable XML output alongside the default HTML so CI tools that consume coverage
        // (SonarQube, Codecov, etc.) can read the report directly without extra config.
        project.getTasks().withType(JacocoReport.class).configureEach(report ->
                report.getReports().getXml().getRequired().set(true)
        );

        // Generate the coverage report after every test task so the report is always fresh.
        project.getTasks().withType(Test.class).configureEach(test ->
                test.finalizedBy(project.getTasks().withType(JacocoReport.class))
        );

        // Fail the build when bundle-level coverage is below the configured threshold. The
        // unscaledVal/scale form converts an integer percentage into a 0.0-1.0 ratio without
        // floating-point loss (e.g. 50 -> 0.50).
        BigDecimal minimumRatio = BigDecimal.valueOf(MINIMUM_COVERAGE_PERCENT, 2);
        project.getTasks().withType(JacocoCoverageVerification.class).configureEach(task ->
                task.violationRules(rules ->
                        rules.rule(rule ->
                                rule.limit(limit -> limit.setMinimum(minimumRatio))
                        )
                )
        );

        // Goal: enforce coverage on `./gradlew build` (and CI) but leave `./gradlew test`
        // free of the threshold so a developer iterating on a change isn't blocked by
        // coverage failures on every test run. `check` is Gradle's standard "run all checks"
        // task and `build` depends on `check`, so wiring verification to `check` produces
        // exactly that split.
        project.getTasks().named("check").configure(check ->
                check.dependsOn(project.getTasks().withType(JacocoCoverageVerification.class))
        );
    }

    /**
     * Forbids any test from importing JUnit's assertion API ({@code Assertions}, {@code Assert},
     * and their static imports). Tests must use AssertJ's {@code assertThat(...)} style instead,
     * which produces clearer failure messages and reads more fluently when chaining checks.
     *
     * <p>The check is wired in as a finalizer on every {@code Test} task, so violations fail
     * the build during normal test runs rather than waiting to be caught in code review.
     */
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

    /**
     * Generates a {@code git.properties} file at the classpath root of every jar so a shipped
     * artifact can be traced back to its exact source commit. Without this, a production jar
     * carries no record of which repo or commit it came from.
     *
     * <p>The key set is deliberately minimal: the remote URL identifies the repo, the commit
     * SHA pins the exact source state, {@code git.commit.id.describe} gives a human-readable
     * release handle (e.g. {@code v1.2.3-4-gabcdef}) for at-a-glance identification, and
     * {@code git.dirty} flags whether the working tree had uncommitted changes at build time.
     * Anything else is recoverable from the repo once the SHA is known.
     *
     * <p>Spring Boot Actuator surfaces these values automatically on {@code /actuator/info}
     * when an app exposes that endpoint.
     */
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

    /**
     * Imports the {@code :platform} project as a Bill of Materials (BOM) into every dependency
     * declaration that contributes to compile, test, or runtime classpaths. A BOM contributes
     * only version constraints — it adds no jars itself — so module build scripts can list
     * dependencies without specifying versions.
     *
     * <p>Centralizing version management in {@code :platform} prevents version drift across
     * modules: bumping a Spring Boot version is one change in one file, not a hunt across
     * every module's build script.
     *
     * <p>The {@code testFixturesImplementation} and {@code developmentOnly} additions are
     * gated on their respective plugins because those declarations only exist when the
     * corresponding plugin is applied.
     */
    private void addPlatformBomToClasspaths(Project project) {
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

    /**
     * Disables the plain {@code jar} task in modules that apply the Spring Boot plugin so
     * only the executable {@code bootJar} fat jar is produced. By default both tasks run and
     * leave two artifacts in {@code build/libs} — a thin library jar and the runnable
     * application jar — and release tooling that globs {@code *.jar} can ship the wrong one.
     *
     * <p>The gate fires only when the Spring Boot plugin is applied; libraries and the
     * platform module continue to produce their plain jar normally.
     */
    private void disablePlainJarInBootApps(Project project) {
        project.getPluginManager().withPlugin("org.springframework.boot", plugin ->
                project.getTasks().named("jar").configure(task -> task.setEnabled(false))
        );
    }
}
