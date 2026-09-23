plugins {
    java
    id("com.diffplug.spotless") version "8.10.2"
}

group = "dev.marshall"
version = "0.1.0-SNAPSHOT"
description = "Hounded – Manhunt: speedrunners vs hunters for Paper"

// Versions chosen and justified in docs/DECISIONS.md.
val paperApiVersion = "26.2.build.129-stable"
val junitVersion = "5.14.4"
val palantirJavaFormatVersion = "2.99.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    // Only so tests can check the bundled YAML files and MiniMessage templates.
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to project.description)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

spotless {
    java {
        palantirJavaFormat(palantirJavaFormatVersion)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
