package com.adibsaikali.gradle.plugins.java;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.diffplug.spotless.LineEnding;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class SpotlessConventions {

    public void apply(Project project) {
        project.getPluginManager().apply(SpotlessPlugin.class);
        SpotlessExtension spotless = project.getExtensions().getByType(SpotlessExtension.class);

        spotless.setEncoding(StandardCharsets.UTF_8);
        spotless.setLineEndings(LineEnding.UNIX);
        spotless.setEnforceCheck(true);

        spotless.java(java -> {
            java.googleJavaFormat();
            java.removeUnusedImports();
            java.licenseHeader(readFile("spotless/template.license.java"));
            java.targetExclude("build/generated/**");
        });

        spotless.sql(sql -> {
            sql.target("**/*.sql");
            String config = readFile("spotless/dbeaver.properties");
            sql.dbeaver().configFile(createTmpFile(config));
        });

        project.getLogger().info("Spotless conventions configured");
    }

    private File createTmpFile(String content) {
        try {
            var file = File.createTempFile("dbeaver", ".properties");
            Files.writeString(file.toPath(), content);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(String path) {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
