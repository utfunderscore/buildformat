plugins {
    id("java")
    `maven-publish`
}

group = "org.readutf.buildformat"
version = "2.0.7"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

subprojects {

    if (project.name == "plugin") {
        return@subprojects
    }

    apply(plugin = "maven-publish")
    apply(plugin = "java-library")

    version = rootProject.version
    group = rootProject.group

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

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
                    username = System.getenv("UTF_MVN_USER") ?: findProperty("utfMvnUser") as String? ?: "readutf"
                    password = System.getenv("UTF_MVN_PASS") ?: findProperty("utfMvnPass") as String? ?: "readutf"
                }
            }

        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.test {
    useJUnitPlatform()
}