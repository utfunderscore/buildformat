pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "buildformat"
include("common")
include("sql")
include("s3")
include("spigot-plugin")
include("hytale-plugin")