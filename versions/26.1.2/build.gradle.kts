plugins {
    id("fabric-loom") version "1.17.0-alpha.7"
}

val minecraftVersion = "26.1.2"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.147.0+26.1.2"
val modMenuVersion = "18.0.0-alpha.8"

base {
    archivesName.set("qolmod-26.1.2")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
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
