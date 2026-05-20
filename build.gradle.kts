plugins {
    java
}

group = "dev.qolmod"
version = "1.0.0"

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.terraformersmc.com/releases/")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
