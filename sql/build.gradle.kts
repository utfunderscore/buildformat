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

buildscript {
    dependencies {
        classpath("org.xerial:sqlite-jdbc:3.50.3.0")
        classpath("org.flywaydb:flyway-database-postgresql:11.3.4")
    }
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.flywaydb:flyway-core:11.14.1")
    testImplementation("org.flywaydb:flyway-database-postgresql:11.14.1")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.postgresql:postgresql:42.7.8")
    testImplementation("org.xerial:sqlite-jdbc:3.48.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.flywaydb:flyway-database-postgresql:11.14.1")

    implementation(project(":common"))

    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")

    implementation("org.jetbrains:annotations:26.0.2")
    api("org.jooq:jooq:3.20.8")
    api("com.zaxxer:HikariCP:7.0.2")

    jooqCodegen("org.postgresql:postgresql:42.7.8")
}

flyway {
    url = "jdbc:postgresql://localhost:5432/builds"
    user = "devuser"
    password = "devpassword"
    driver = "org.postgresql.Driver"
    cleanDisabled = false
    locations = arrayOf("filesystem:src/main/resources/db/migration/")
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/builds"
            user = "devuser"
            password = "devpassword"
        }
        generator {

            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
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

tasks.test {
    useJUnitPlatform()
}
