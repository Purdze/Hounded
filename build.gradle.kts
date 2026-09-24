plugins {
    java
    id("com.diffplug.spotless") version "8.10.2"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.marshall"
version = "1.0.0"
description = "Hounded - Manhunt: speedrunners vs hunters for Paper"

// Compiled against the oldest supported Paper API, tested against the newest (see docs/DECISIONS.md).
val oldestMinecraftVersion = "1.21.4"
val oldestJavaVersion = 21
val newestMinecraftVersion = "26.2"
val oldestPaperApiVersion = "$oldestMinecraftVersion-R0.1-SNAPSHOT"
val paperApiVersion = "$newestMinecraftVersion.build.129-stable"
val junitVersion = "6.1.3"
val mockBukkitVersion = "4.116.1"
val placeholderApiVersion = "2.12.3"
val palantirJavaFormatVersion = "2.99.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    // PlaceholderAPI comes only from its own repository, and that repository serves nothing else.
    exclusiveContent {
        forRepository {
            maven("https://repo.helpch.at/releases/") {
                name = "helpch"
            }
        }
        filter {
            includeGroup("me.clip")
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$oldestPaperApiVersion")
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

// Paper 1.21.x runs on Java 21; the Java 25 toolchain is only needed to run the tests on 26.2.
tasks.compileJava {
    options.release.set(oldestJavaVersion)
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

// GPL-3.0 expects the license to travel with the plugin jar.
tasks.jar {
    from("LICENSE")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "description" to project.description,
        "apiVersion" to oldestMinecraftVersion,
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<xyz.jpenilla.runpaper.task.RunServer>().configureEach {
    // The owner accepted the Minecraft EULA on 2026-09-23.
    jvmArgs("-Dcom.mojang.eula.agree=true")
    // Only for trying the placeholders on the dev server; Hounded doesn't ship it.
    downloadPlugins {
        hangar("PlaceholderAPI", placeholderApiVersion)
    }
}

tasks.runServer {
    minecraftVersion(newestMinecraftVersion)
    runDirectory = layout.projectDirectory.dir("test-server")
}

// The oldest supported version, on the Java it requires, in its own folder.
tasks.register<xyz.jpenilla.runpaper.task.RunServer>("runServerOldest") {
    minecraftVersion(oldestMinecraftVersion)
    runDirectory = layout.projectDirectory.dir("test-server-$oldestMinecraftVersion")
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(oldestJavaVersion) }
    pluginJars(tasks.jar.flatMap { it.archiveFile })
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
