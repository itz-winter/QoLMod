plugins {
    java
}

version = rootProject.version

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

// Common module has no Minecraft dependency — only plain Java code
// Version-specific modules will depend on this
dependencies {
    compileOnly("com.google.code.gson:gson:2.10.1")
}
