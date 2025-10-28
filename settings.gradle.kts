pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "buildformat"
include("common")
include("sqlite")
include("plugin")
include("s3")