import java.io.ByteArrayOutputStream

plugins {
    application
    `java-library`
    `maven-publish`

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

fun getGitCommit(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }

    val commitHash = stdout.toString().trim()
    println("Current short commit hash: $commitHash")

    return commitHash
}

group = "tk.booky"
version = "1.0.0+${getGitCommit()}"

val main = "${group}.${name.toLowerCase()}.${name}Main"

repositories {
    mavenCentral()
}

dependencies {
    api("com.discord4j:discord4j-core:3.2.1")
    api("com.google.code.gson:gson:2.8.9")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = project.name.toLowerCase()
        from(components["java"])
    }
}

tasks {
    jar {
        manifest.attributes(
            "Implementation-Version" to project.version,
            "Implementation-Title" to project.name,
            "Implementation-Vendor" to "booky10",
            "Main-Class" to main,
        )
    }

    build {
        dependsOn(shadowJar)
    }
}

application {
    mainClass.set(main)
}
