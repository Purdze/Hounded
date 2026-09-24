plugins {
    java
    id("com.diffplug.spotless") version "8.10.2"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.marshall"
version = "0.1.0-SNAPSHOT"
description = "Hounded – Manhunt: speedrunners vs hunters for Paper"

val paperApiVersion = "26.2.build.129-stable"
val junitVersion = "6.1.3"
val mockBukkitVersion = "4.116.1"
val placeholderApiVersion = "2.12.3"
val palantirJavaFormatVersion = "2.99.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.helpch.at/releases/") {
        name = "helpch"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:$mockBukkitVersion")
    testImplementation("me.clip:placeholderapi:$placeholderApiVersion")
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
    // MockBukkit reports server features it doesn't simulate as skipped tests. Fail instead, so a
    // test that never ran can't pass silently.
    afterSuite(
        KotlinClosure2<TestDescriptor, TestResult, Unit>({ suite, result ->
            if (suite.parent == null && result.skippedTestCount > 0) {
                throw GradleException("${result.skippedTestCount} test(s) were skipped; see the test report")
            }
        }),
    )
}

tasks.processResources {
    val props = mapOf("version" to project.version, "description" to project.description)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion("26.2")
    runDirectory = layout.projectDirectory.dir("test-server")
    // The owner accepted the Minecraft EULA on 2026-09-23.
    jvmArgs("-Dcom.mojang.eula.agree=true")
    // Only for trying the placeholders on the dev server; Hounded doesn't ship it.
    downloadPlugins {
        hangar("PlaceholderAPI", placeholderApiVersion)
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
