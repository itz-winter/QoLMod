plugins {
    id("fabric-loom") version "1.16.1"
}

val minecraftVersion = "1.21.2"
val yarnMappings = "1.21.2+build.1"
val loaderVersion = "0.16.9"
val fabricApiVersion = "0.106.1+1.21.2"
val modMenuVersion = "12.0.0-beta.1"

base {
    archivesName.set("qolmod-1.21.2")
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
