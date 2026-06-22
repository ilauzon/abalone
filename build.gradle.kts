plugins {
    kotlin("jvm") version "2.4.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src")
        }
    }
}

dependencies {
    implementation("com.varabyte.kotter:kotter:1.3.0")
    testImplementation("com.varabyte.kotterx:kotter-test-support:1.3.0")
}

application {
    applicationDefaultJvmArgs = listOf(
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--enable-native-access=ALL-UNNAMED",
    )
    mainClass.set("MainKt")
}
