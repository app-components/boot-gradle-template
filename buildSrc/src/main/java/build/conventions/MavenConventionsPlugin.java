package build.conventions;


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

abstract public class MavenConventionsPlugin implements Plugin<Project> {

    private final String REPO_URL = "https://maven.pkg.github.com/asaikali/app-components";
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(MavenPublishPlugin.class);

        PublishingExtension publishingExtension  = project.getExtensions().getByType(PublishingExtension.class);
        publishingExtension.publications(publications -> publications.register("maven", MavenPublication.class, mavenPublication -> {

            project.getPluginManager().withPlugin("java", plugin -> {
                mavenPublication.from(project.getComponents().getByName("java"));
            });

            project.getPluginManager().withPlugin("java-platform", plugin -> {
                mavenPublication.from(project.getComponents().getByName("javaPlatform"));
            });

            mavenPublication.getPom().getUrl().set(REPO_URL);
        }));

        publishingExtension.repositories(repositories -> repositories.maven(mavenRepository -> {
            mavenRepository.setName("GitHubPackages");
            mavenRepository.setUrl(REPO_URL);
            mavenRepository.credentials(creds -> {
                creds.setUsername(System.getenv("GITHUB_ACTOR"));
                creds.setPassword(System.getenv("GITHUB_TOKEN"));
            });
        }));
    }
}

/*

Gradle script that corresponds to the above Plugin implementation:

id("maven-publish")

publishing {
    publications.create<MavenPublication>("github") {
        from(components["javaPlatform"])

        pom {
            url.set("https://maven.pkg.github.com/asaikali/app-components")
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/asaikali/app-components")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
 */
