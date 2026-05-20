plugins {
    id("fabric-loom") version "1.16.1"
}

val minecraftVersion = "1.21.11"
val yarnMappings = "1.21.11+build.5"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.141.3+1.21.11"
val modMenuVersion = "17.0.0"

base {
    archivesName.set("qolmod-1.21.11")
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
