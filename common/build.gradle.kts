plugins {
    id("java")
}

group = "org.readutf.buildstore"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}