plugins {
    id("java-library")
    id("org.flywaydb.flyway") version "11.14.1"
    id("org.jooq.jooq-codegen-gradle") version "3.20.8"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://download.red-gate.com/maven/release")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.xerial:sqlite-jdbc:3.50.3.0")
    testImplementation("org.flywaydb:flyway-core:11.14.1")
    testImplementation("org.flywaydb:flyway-database-postgresql:11.14.1")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.postgresql:postgresql:42.7.8")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(project(":common"))

    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")

    compileOnly("org.xerial:sqlite-jdbc:3.50.3.0")
    jooqCodegen("org.xerial:sqlite-jdbc:3.50.3.0")


    implementation("org.jetbrains:annotations:26.0.2")
    api("org.jooq:jooq:3.20.8")
    api("com.zaxxer:HikariCP:7.0.2")
}

buildscript {
    dependencies {
        classpath("org.xerial:sqlite-jdbc:3.50.3.0")
        classpath("org.flywaydb:flyway-database-postgresql:11.3.4")
    }
}

flyway {
    url = "jdbc:sqlite:${project.projectDir}/testdb.sqlite"
    driver = "org.sqlite.JDBC"
    cleanDisabled = false
    locations = arrayOf("filesystem:src/main/resources/db/migration/sqlite")
}
jooq {
    // Example configuration - adjust as needed
    configuration {
        jdbc {
            driver = "org.sqlite.JDBC"
            url = "jdbc:sqlite:${project.projectDir}/testdb.sqlite"
        }
        generator {
            database {
                name = "org.jooq.meta.sqlite.SQLiteDatabase"
            }
            generate {
                isDeprecated = false
                isRecords = true
                isFluentSetters = true
            }
            target {
                packageName = "org.readutf.buildformat.postgres.jooq"
                directory = "src/main/java"
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn("jooqCodegen")
}

tasks.test {
    useJUnitPlatform()
}