# QoLMod: Quality-of-Life features

QoLMod provides optional, configurable quality-of-life utilities to make singleplayer Minecraft more pleasant. Features are designed to be lightweight and toggleable.

## What QoLMod has

Features:

- Fullbright - toggle darkness removal with a keybind; HUD shows status.
- Tree Chopper - hold the keybind to fell whole trees quickly.
- Trade Refresh - refresh villager trades in singleplayer worlds.
- Accurate Block Placement - avoids accidental block rotations when placing against entities.
- Recipe Viewer - view crafting recipe ingredients in chat (command available).
- InvMove - move while GUI screens (inventory/menus) are open.
- Hunger HUD - numeric hunger and saturation display on-screen.
- Workbench - open a crafting table anywhere (singleplayer command).
- Villager-in-a-bucket utilities (capture/release villagers; available 1.21.9+ where implemented).

All of these can be toggled in the config or via ModMenu when installed.

## Quick install

1. Download the JAR that matches your Minecraft version.
2. Install Fabric Loader and Fabric API for that Minecraft version.
3. Place the JAR into your `mods/` folder.
4. (Optional) Install ModMenu to adjust settings in-game.

This mod runs on Fabric client and works in singleplayer when Fabric is present.

## Configuration

Config file: `config/qolmod.json` (created on first run). Example defaults:

```json
{
  "fullbright.enabled": false,
  "treeChopper.enabled": true,
  "tradeRefresh.enabled": true,
  "accurateBlockPlacement.enabled": true,
  "recipeViewer.enabled": true,
  "invMove.enabled": true,
  "hungerDisplay.enabled": true,
  "villagerBucketEnabled": true,
  "villagerBucketZombieEnabled": false,
  "overrideOtherMods": false
}
```

If you have ModMenu installed, use the in-game config screen to toggle features without editing files.

## Commands & Keybinds

Singleplayer commands (available when running Fabric client):

- `/workbench` - open a crafting table interface anywhere.
- `/recipe <item>` - show a crafting recipe in chat (when available).

Client keybinds (defaults may vary by version):

- Fullbright toggle - toggles fullbright on/off.
- Tree Chopper hold - hold while swinging to fell trees.

Most client features also have optional HUD elements and can be toggled independently.

## Supported Minecraft versions

Stable singleplayer support is provided for multiple Minecraft versions on Fabric (notably 1.16.5 through 1.21.11). Always choose the JAR that matches your Minecraft client version.

| Version | Supported? |
|---------|------------|
| 1.16.5  | Yes        |
| 1.17.x  | Yes        |
| 1.18.x  | Yes        |
| 1.19.x  | Yes        |
| 1.20.x  | Yes        |
| 1.21.x  | Yes        |
| 26.1.x  | Not Yet    |

Native version(s): 1.21.4-11.

## Troubleshooting

- If a client-side feature conflicts with another mod, try toggling `overrideOtherMods` in `config/qolmod.json` and restart Minecraft.
- Confirm Fabric Loader and Fabric API versions match your Minecraft client.
- Review `logs/latest.log` for error messages and include relevant snippets when reporting issues.

## Privacy

QoLMod does not collect telemetry. Optional integrations (e.g., Discord webhooks) are disabled by default and documented in config if present.

## License

MIT - see the `LICENSE` file in this repository.

---

Want a shorter blurb for Modrinth's description field or a trimmed feature list for the mod page? Tell me the desired length (e.g., 200 or 400 characters) and I will produce it.
