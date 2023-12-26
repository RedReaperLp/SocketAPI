plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("application")
}

group = "com.github.redreaperlp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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
