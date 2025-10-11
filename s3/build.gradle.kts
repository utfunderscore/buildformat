plugins {
    id("java")
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:localstack:1.21.3")

    implementation(project(":common"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")

    api("software.amazon.awssdk:aws-core:2.31.77")
    api("software.amazon.awssdk:s3:2.31.77")
    api("software.amazon.awssdk:s3-transfer-manager:2.31.77")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.test {
    useJUnitPlatform()
}
