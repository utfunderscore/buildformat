plugins {
    `java-library`
    kotlin("jvm") version "2.1.21"
}


repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    api(project(":common"))
    api("org.jetbrains.exposed:exposed-core:0.61.0")
    api("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    api("com.zaxxer:HikariCP:6.3.0")


    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("com.zaxxer:HikariCP:6.3.0")
    testImplementation("org.postgresql:postgresql:42.7.5")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}