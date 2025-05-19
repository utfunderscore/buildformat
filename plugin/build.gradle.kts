import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

group = "io.papermc.paperweight"
version = "1.0.0-SNAPSHOT"
description = "Test plugin for paperweight-userdev"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    maven { url = uri("https://repo.panda-lang.org/releases") }
}

dependencies {
    implementation(project(":common"))
    implementation("dev.rollczi:litecommands-bukkit:3.9.7")

    compileOnly(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }

    implementation(project(":postgres"))
    implementation(project(":s3"))


    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
}

tasks {
    compileJava {
        options.release = 21
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}

bukkitPluginYaml {
    name = "buildformat"
    main = "org.readutf.buildformat.plugin.BuildFormatPlugin"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.add("Author")
    apiVersion = "1.21.4"
    depend = listOf("FastAsyncWorldEdit")
}