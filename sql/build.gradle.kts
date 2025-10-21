plugins {
    kotlin("jvm") version "2.2.20"
    id("java-library")
}

group = "org.readutf.buildformat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    api("org.jetbrains.exposed:exposed:0.17.14")
    api("org.jetbrains.exposed:exposed:0.17.14")
    api("org.xerial:sqlite-jdbc:3.50.3.0")
    api("com.zaxxer:HikariCP:7.0.2")

    api("org.tinylog:tinylog-api:2.7.0")
    api("org.tinylog:tinylog-impl:2.7.0")
    api("org.tinylog:slf4j-tinylog:2.7.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}