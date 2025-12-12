import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.panda-lang.org/releases") }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://mvn.utf.lol/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":sql"))
    implementation(project(":s3"))

    implementation("dev.rollczi:litecommands-bukkit:3.9.7")
    implementation("net.kyori:adventure-text-minimessage:4.25.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.25.0")

    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.14.0")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.14.0")

    implementation("org.flywaydb:flyway-database-postgresql:11.19.0")

    implementation("net.minestom:minestom:2025.10.18-1.21.10")
    implementation("dev.hollowcube:schem:2.0.0")
    implementation("dev.hollowcube:polar:1.15.0")
    implementation("org.postgresql:postgresql:42.7.8")

    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
}

tasks.runServer {
    minecraftVersion("1.21.10")
    jvmArgs(
        "-Xmx2G",
    )
}

tasks {
    compileJava {
        options.release = 25
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


bukkitPluginYaml {
    name = "buildformat"
    main = "org.readutf.buildformat.BuildFormatPlugin"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.add("Author")
    apiVersion = "1.21"
}