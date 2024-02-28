plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
    id("de.chojo.publishdata") version "1.4.0"
}

group = "com.github.redreaperlp"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://eldonexus.de/repository/maven-public/")
    maven("https://eldonexus.de/repository/maven-proxies/")
}
dependencies {
    implementation("org.json", "json", "20230618")
}

tasks {
    shadowJar {

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


if (!project.version.toString().contains("SNAPSHOT")) {
    java {
        withSourcesJar()
        withJavadocJar()
    }
}

tasks.withType<Javadoc>().configureEach() {
    isFailOnError = false
}

