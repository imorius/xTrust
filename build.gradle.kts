plugins {
    kotlin("jvm") version "2.5.0-Beta1"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("de.exlll:configlib-yaml:4.8.1")
    implementation("com.tcoded:FoliaLib:0.5.1")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("dev.jorel:commandapi-paper-shade:12.0.0")
    implementation("dev.jorel:commandapi-kotlin-paper:12.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        relocate("com.tcoded.folialib", "me.orius.xtrust.lib.folialib")
        relocate("org.apache.commons.compress", "me.orius.xtrust.lib.commons.compress")
        relocate("dev.jorel.commandapi", "me.orius.xtrust.lib.commandapi")
    }

    runServer { // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
