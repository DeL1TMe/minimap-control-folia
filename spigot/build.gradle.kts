plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.2"
}

val versionStr = (System.getenv("VERSION")?: "v1.0.0").removePrefix("v")
val pluginVersion = versionStr

group = "com.funniray.minimap"
version = versionStr

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        url = uri("https://repo.viaversion.com")
    }
}

dependencies {
    // Main Dependencies
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.19-alpha")
    compileOnly("com.viaversion:viaversion-api:5.0.1")
    implementation(project(":common"))

    // Common Dependencies
    implementation("org.spongepowered:configurate-core:4.2.0")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("net.kyori:adventure-api:4.22.0")
    implementation("net.kyori:adventure-platform-bukkit:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.22.0")
    implementation("net.kyori:adventure-nbt:4.22.0")
}

val javaTarget = 25
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaTarget))
        vendor.set(JvmVendorSpec.matching(""))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(javaTarget)
    }
    processResources {
        inputs.property("version", pluginVersion)
        filesMatching("plugin.yml") {
            expand("version" to pluginVersion)
        }
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        relocate("org.spongepowered.configurate", "com.funniray.minimap.configurate")
        relocate("net.kyori", "com.funniray.minimap.kyori")
        relocate("io.leangen.geantyref", "com.funniray.minimap.geantyref")
        exclude("com/google/gson/**")
        exclude("org/apache/commons/**")
        exclude("org/yaml/snakeyaml/**")
        archiveBaseName.set("minimap-control-folia")
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }
}
