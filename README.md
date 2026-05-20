# QoLModQoLMod — Quality-of-life features for Fabric

===========================================

A Fabric quality-of-life mod covering both client-side utilities and server-side commands. Supports Minecraft 1.16.5 through 1.21.11.

A compact, factual description of what this mod (the code in this repository) provides. The list below contains only features implemented in the source tree under `versions/*/src/main/java`.

---

Features

## Features--------



### Client-sideServer-side

| Feature | Description | Toggle |

|---|---|---|- Homes

| **Fullbright** | Removes darkness at night and underground | `G` keybind, or via config |  - Commands: `/home [name]`, `/sethome [name]`, `/delhome <name>`, `/homes`.

| **Tree Chopper** | Fells entire trees with one swing while the keybind is held | Config |  - Stores per-player homes (name, position, rotation, dimension) and teleports players to them.

| **Trade Refresh** | Restores merchant trades in singleplayer | Config |  - Enforces a configurable maximum homes-per-player.

| **Accurate Block Placement** | Prevents unintended block rotations when placing against entities | Config |

| **Recipe Viewer** | `/recipe <item>` shows crafting ingredients in chat | Config |- Ban/unban

| **InvMove** | Allows movement while inventory GUIs are open | Config |  - Commands: `/ban <player> [duration] [reason]`, `/unban <player>`, `/pardon` (alias).

| **Hunger Display HUD** | Renders numeric food level and saturation on-screen | Config |  - Supports permanent and temporary bans using the server ban list.



### Server-sideClient-side

| Feature | Commands | Notes |

|---|---|---|- Keybound client utilities (registered in `QoLModClient`):

| **Homes** | `/home [name]`, `/sethome [name]`, `/delhome <name>`, `/homes` | Per-player, multi-home, configurable limit |  - Fullbright toggle (keybind + HUD support).

| **TPA** | `/tpa <player>`, `/tpaccept`, `/tpdeny` | Request-based teleport |  - Tree chopper keybind/handler.

| **Back** | `/back` | Returns to last death or pre-teleport position |  - Trade refresh keybind/handler.

| **Workbench** | `/workbench` | Opens a crafting table anywhere |  - Inventory movement while GUIs are open (InvMove) keybind/handler.

| **PvP toggle** | `/pvp` | Per-player PvP opt-in/out |  - Accurate block placement helper.

| **Ban / Unban** | `/ban <player> [duration] [reason]`, `/unban <player>` | Supports temporary bans (`10m`, `2h`, `7d`, etc.) |

| **Vanish** | `/vanish` | Makes operators invisible to other players |- HUD

| **Invsee** | `/invsee <player>` | View and edit another player's inventory |  - Hunger display renderer.



### Entity Features (1.21.9+)Integrations & utilities

| Feature | Description | Config |

|---|---|---|- Integrations (server-side where implemented): placeholder / platform integrations are implemented on the server side in the codebase when present. This project ships server-side integration code per-version in `versions/*/src/main/java/dev/qolmod` when available.

| **Villager in a Bucket** | Right-click a villager with an empty bucket to capture it; right-click a surface to release. Preserves profession, level, and trades. | `villagerBucketEnabled` |- Utility classes present: `DurationParser`, `FileManager`, `MessageFormatter`, `VersionHelper`, and other helpers used by the server- and client-side features.

| **Zombie Villager in a Bucket** | Same as above for zombie villagers | `villagerBucketZombieEnabled` |

| **Wandering Trader in a Bucket** | Same as above for wandering traders | `villagerBucketEnabled` |Project notes

-------------

### Integrations

- **Discord** — optional webhook integration for chat relay- Multi-version setup: each supported Minecraft version is a subproject under `versions/<mc-version>/`.

- **LuckPerms** — prefix/suffix support in chat channels- Each subproject contains Fabric/Loom Gradle configuration and version-specific sources and resources.

- **PlaceholderAPI** — custom placeholders via `/papi`- Some experimental/26.x subprojects exist but may require additional configuration (they use experimental Loom mappings in the tree).



---Want more detail?

------------------

## Requirements

If you'd like a Modrinth-ready description trimmed to a specific length, or want example usages for each command, or the configuration options documented from `config.yml`, tell me which one and I will expand the README accordingly.

- **Minecraft** — see the supported versions table below
- **Fabric Loader** ≥ 0.15
- **Fabric API** — bundled per-version
- **ModMenu** *(optional)* — for the in-game config screen

---

## Supported Versions

| Minecraft | Status |
|---|---|
| 1.21.11 | ✅ Active |
| 1.21.10 | ✅ Active |
| 1.21.9 | ✅ Active |
| 1.21.8 | ✅ Maintained |
| 1.21.7 | ✅ Maintained |
| 1.21.6 | ✅ Maintained |
| 1.21.5 | ✅ Maintained |
| 1.21.4 | ✅ Maintained |
| 1.21.3 | ✅ Maintained |
| 1.21.1 | ✅ Maintained |
| 1.20.6 | ✅ Maintained |
| 1.20.4 | ✅ Maintained |
| 1.20.1 | ✅ Maintained |
| 1.19.4 | ✅ Maintained |
| 1.19.2 | ✅ Maintained |
| 1.18.2 | ✅ Maintained |
| 1.17.1 | ✅ Maintained |
| 1.16.5 | ✅ Maintained |

---

## Building

**Requirements:** JDK 21+ (path set via `JAVA_HOME` or `gradle.properties`).

### Windows
```bat
build.bat              :: build all versions
build.bat 1.21.11      :: build one version
```

### Linux / macOS
```bash
chmod +x build.sh
./build.sh             # build all versions
./build.sh 1.21.11     # build one version
./build.sh clean       # clean outputs
```

### Gradle directly
```bash
./gradlew :versions:1.21.11:build -x test
./gradlew build -x test          # all versions
```

Output JARs land in `versions/<mc-version>/build/libs/`.

---

## Configuration

Open the in-game config screen via **ModMenu**, or edit `config/qolmod.json` manually.

| Key | Default | Description |
|---|---|---|
| `fullbright.enabled` | `false` | Toggle fullbright |
| `treeChopper.enabled` | `true` | Toggle tree chopper |
| `tradeRefresh.enabled` | `true` | Toggle trade refresh |
| `accurateBlockPlacement.enabled` | `true` | Toggle accurate block placement |
| `recipeViewer.enabled` | `true` | Toggle recipe viewer |
| `invMove.enabled` | `true` | Toggle inventory movement |
| `hungerDisplay.enabled` | `true` | Toggle hunger HUD |
| `villagerBucketEnabled` | `true` | Villager/trader bucketing *(1.21.9+)* |
| `villagerBucketZombieEnabled` | `false` | Zombie villager bucketing *(1.21.9+)* |
| `overrideOtherMods` | `false` | Override conflicting mods (restart required) |

---

## Project Structure

```
QoLMod/
├── common/                  # Shared sources (config, utilities)
├── versions/
│   ├── 1.21.11/             # Per-version Fabric subproject
│   │   └── src/main/
│   │       ├── java/dev/qolmod/
│   │       │   ├── mixin/       # Version-specific mixins
│   │       │   ├── item/        # Custom items (VillagerBucketItem, etc.)
│   │       │   └── client/      # Client code & ModMenu screen
│   │       └── resources/
│   │           └── assets/qolmod/
│   │               ├── models/item/
│   │               └── textures/item/
│   └── ...
├── build.bat                # Windows build helper
├── build.sh                 # Linux/macOS build helper
├── build.gradle.kts
└── settings.gradle.kts
```

---

## License

MIT — see [LICENSE](LICENSE) for details.
