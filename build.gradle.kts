plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.herzchen"
version = "1.2.01"

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileJava {
        targetCompatibility = "17"
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        manifest.attributes["Main-Class"] = "com.herzchen.moreenchant.MoreEnchant"
    }
}