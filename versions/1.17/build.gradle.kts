plugins {
    id("fabric-loom") version "1.16.1"
}

val minecraftVersion = "1.17"
val yarnMappings = "1.17+build.13"
val loaderVersion = "0.16.9"
val fabricApiVersion = "0.46.1+1.17"
val modMenuVersion = "2.0.17"

base {
    archivesName.set("qolmod-1.17")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.terraformersmc:modmenu:$modMenuVersion")

    implementation(project(":common"))
    include(project(":common"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}
