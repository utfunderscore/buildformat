plugins {
    id("java-library")

}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:localstack:1.19.8")
    testImplementation(project(":common"))
    testImplementation("org.testcontainers:postgresql:1.19.8")

    compileOnly(project(":common"))
    api("software.amazon.awssdk:s3:2.36.2")
    api("software.amazon.awssdk:s3-transfer-manager:2.36.2")
}

tasks.test {
    useJUnitPlatform()
}