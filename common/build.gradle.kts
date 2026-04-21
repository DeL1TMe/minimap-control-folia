plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
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
}

dependencies {
    compileOnly("com.google.guava:guava:33.4.8-jre")
    compileOnly("com.google.code.gson:gson:2.13.1")
    implementation("org.spongepowered:configurate-core:4.2.0")
    implementation("net.kyori:adventure-api:4.22.0")
    implementation("net.kyori:adventure-text-minimessage:4.22.0")
    implementation("net.kyori:adventure-nbt:4.22.0")
}
