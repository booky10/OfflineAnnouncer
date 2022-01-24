plugins {
    `java-library`
    `maven-publish`
}

group = "tk.booky"
version = "1.0.0"

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
