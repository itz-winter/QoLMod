plugins {
    id("fabric-loom") version "1.16.1"
}

val minecraftVersion = "1.21.9"
val yarnMappings = "1.21.9+build.1"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.134.1+1.21.9"
val modMenuVersion = "16.0.1"

base {
    archivesName.set("qolmod-1.21.9")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")

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
