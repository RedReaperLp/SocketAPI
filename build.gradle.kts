import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import de.chojo.PublishData

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("application")
    id("maven-publish")
    id("de.chojo.publishdata") version "1.4.0"
}

group = "com.github.redreaperlp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-proxies/")
}
dependencies {
    implementation("org.json", "json", "20230618")
}

application {
    mainClass.set("com.github.redreaperlp.Main")
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "com.github.redreaperlp.Main"
        }
    }
}

publishData {
    useEldoNexusRepos()
    publishComponent("java")
}

publishing {
    publications.create<MavenPublication>("maven") {
        publishData.configurePublication(this)
    }


    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    username = System.getenv("NEXUS_USERNAME")
                    password = System.getenv("NEXUS_PASSWORD")
                }
            }

            name = "EldoNexus"
            url = uri(publishData.getRepository())
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach() {
    isFailOnError = false
}