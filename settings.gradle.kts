pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "buildformat"
include("common")
include("sql")
include("plugin")
include("s3")