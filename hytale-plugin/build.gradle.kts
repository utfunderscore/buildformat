plugins {
    id("java")
}

group = "org.readutf.buildformat"
version = "dev"

repositories {
    mavenCentral()
    maven("https://mvn.utf.lol/releases")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(project(":common"))
    implementation(project(":sql"))
    implementation(project(":s3"))

    implementation("com.hypixel.hytale:Server:2026.01.17")
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("org.flywaydb:flyway-database-postgresql:11.19.0")

}

tasks.test {
    useJUnitPlatform()
}

var serverRunDir = objects.directoryProperty()
serverRunDir.set(file("$projectDir/run"))
val hytaleHome = "/home/connor/.var/app/com.hypixel.HytaleLauncher/data/Hytale/"
val patchline = "release"

tasks.register<JavaExec>("runHytaleServer") {
    group = "application"
    description = "Runs the Hytale server"

    mainClass.set("com.hypixel.hytale.Main")
    classpath = sourceSets.main.get().runtimeClasspath

    val params = mutableListOf("--allow-op", "--assets=$hytaleHome/install/$patchline/package/game/latest/Assets.zip", "--disable-sentry")
    standardInput = System.`in`

    val runDir = serverRunDir.get()
    runDir.asFile.mkdirs()
    workingDir = runDir.asFile

    args = params
}