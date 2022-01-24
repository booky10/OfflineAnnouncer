plugins {
    application
    `java-library`
    `maven-publish`

    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "tk.booky"
version = "1.0.0"

val main = "${group}.${name.toLowerCase()}.${name}Main"

repositories {
    mavenCentral()
}

dependencies {
    api("com.discord4j:discord4j-core:3.2.1")
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
