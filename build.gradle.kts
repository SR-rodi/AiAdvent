import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "ru.sr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvm()
    jvmToolchain(24)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                val ktorVersion = "2.3.5"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("io.insert-koin:koin-core:3.5.3")
                implementation("io.insert-koin:koin-compose:1.1.2")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                val ktorVersion = "2.3.5"
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.4.11")
            }
        }
        val jvmTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.sr.MainDesktopKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")
        jvmArgs += listOf(
            "-DDEEP_SEEK_API_KEY=${findProperty("DEEPSEEK_API_KEY") ?: ""}",
            "-DOPEN_ROUTER_API_KEY=${findProperty("OPEN_ROUTER_API_KEY") ?: ""}",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "AiAdvent"
            packageVersion = "1.0.0"
        }
    }
}
