pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "QoLMod"

include("common")
include("versions:1.16.5")
include("versions:1.17")
include("versions:1.17.1")
include("versions:1.18")
include("versions:1.18.1")
include("versions:1.18.2")
include("versions:1.19")
include("versions:1.19.1")
include("versions:1.19.2")
include("versions:1.19.3")
include("versions:1.19.4")
include("versions:1.20")
include("versions:1.20.1")
include("versions:1.20.2")
include("versions:1.20.3")
include("versions:1.20.4")
include("versions:1.20.5")
include("versions:1.20.6")
include("versions:1.21")
include("versions:1.21.1")
include("versions:1.21.2")
include("versions:1.21.3")
include("versions:1.21.4")
include("versions:1.21.5")
include("versions:1.21.6")
include("versions:1.21.7")
include("versions:1.21.8")
include("versions:1.21.9")
include("versions:1.21.10")
include("versions:1.21.11")
// 26.1, 26.1.1, 26.1.2 cannot be built yet - Fabric Intermediary mappings not available
