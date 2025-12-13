plugins {
    id("java-library")
}


repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")

    api("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    api("org.jetbrains:annotations:26.0.2")
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat. FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }
}


tasks {
    compileJava {
        options.release = 25
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}


tasks.test {
    useJUnitPlatform()
}