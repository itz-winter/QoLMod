plugins {
    id("fabric-loom") version "1.16.1"
}

val minecraftVersion = "1.21.7"
val yarnMappings = "1.21.7+build.8"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.129.0+1.21.7"
val modMenuVersion = "15.0.2"

base {
    archivesName.set("qolmod-1.21.7")
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

tasks.withType<JavaCompile> {
    options.release = 21
}
