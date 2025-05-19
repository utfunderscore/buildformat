plugins {
    id("java-library")
    id("org.jooq.jooq-codegen-gradle") version "3.20.4"
}

group = "org.readutf.buildstore"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":common"))

    api("com.zaxxer:HikariCP:6.3.0")
    api("org.jooq:jooq:3.20.4")
    api("org.postgresql:postgresql:42.7.5")
    api("org.flywaydb:flyway-database-postgresql:11.8.2")

    jooqCodegen("org.postgresql:postgresql:42.7.5")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/example_db"
            user = "postgres"
            password = "password"
        }

        generator {
            name = "org.jooq.codegen.DefaultGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
            }
            target {
                packageName = "org.readutf.buildstore.generated"
                directory = "src/main/java"
            }
        }
    }

}

tasks.test {
    useJUnitPlatform()
}