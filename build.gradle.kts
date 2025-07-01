plugins {
    `java-library`
    `maven-publish`
}

group = "org.readutf.buildformat"
version = "1.0.17"

repositories {
    mavenCentral()
}

subprojects {

    if (project.name == "plugin") {
        return@subprojects
    }

    apply(plugin = "maven-publish")
    apply(plugin = "java-library")

    version = rootProject.version
    group = rootProject.group

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }

        repositories {
            maven {
                name = "utfMvn"
                url = uri("https://mvn.utf.lol/releases")
                credentials {
                    username = System.getenv("UTF_MVN_USER")
                    password = System.getenv("UTF_MVN_PASS")
                }
            }

        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
