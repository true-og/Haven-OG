plugins {
    id("java")
    id("com.diffplug.spotless") version "8.1.0"
    id("checkstyle")
    eclipse
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

group = "net.trueog.haven-og"

version = "1.0.0"

val apiVersion = "1.19"

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
    from("LICENSE") { into("/") }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:deprecation")
    options.encoding = "UTF-8"
}

spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml")
        leadingTabsToSpaces()
        removeUnusedImports()
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts")
    }
}

checkstyle {
    toolVersion = "10.18.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    isShowViolations = true
}

tasks.named("compileJava") { dependsOn("spotlessApply") }

tasks.named("spotlessCheck") { dependsOn("spotlessApply") }
