# **Auto Attack | Auto Plant Mod**

## **Primary Purpose**

A lightweight, event-driven automation tool designed for early- to mid-game technical farms—such as experience farms, semi-automatic Piglin bartering setups, mob grinders, and tree farms. Supports automatic attacking, planting, bone meal acceleration, and intelligent audio control.

Fully compatible with **Minecraft 1.21.1 through 1.21.11**, featuring a refactored codebase for improved stability and maintainability.

## **Key Controls (Default Keybinds)**

- **F7**: Toggle **master volume mute/unmute** instantly — your global audio switch.
- **F8**: Toggle **Auto Attack** on/off.
- **F9**: Toggle **Auto Planting** on/off.
- **F10**: Open the **in-game configuration GUI** to customize all mod settings in real time.

Simply aim at a valid target and press the corresponding key—the mod handles the rest automatically, without simulating mouse input or hijacking your cursor.

## **Compatible Targets**

- **Auto Attack** works on armor stands and hostile mobs. Neutral and passive mobs can be enabled via settings.
- **Auto Planting** supports all Overworld and Nether plants and saplings. For full automation, hold the plant/sapling in your main hand and bone meal in your off-hand—the mod will plant and fertilize automatically.

## **Smart Audio Management**

The mod features an intelligent **auto-mute system** with three priority levels:

1. **Manual (Highest Priority)**: Press **F7** to force mute or restore master volume at any time.
2. **Linked (Medium Priority)**: Automatically mutes when Auto Attack or Auto Planting is active (configurable).
3. **Focus-Based (Lowest Priority)**: Mutes when the Minecraft window loses focus or is minimized, and restores volume when you return.

> **Known Issue**: If the game is forcefully closed via the launcher (e.g., "Close Game" button in MultiMC, Prism, or the official launcher) while muted, the audio may remain stuck at zero after restarting Minecraft.
>
> **Cause**: The mod stores the original volume in memory and relies on Minecraft’s shutdown event to restore it. When the game process is terminated externally (not via in-game exit), this event is never fired, so the volume isn’t restored. Additionally, the mod’s runtime data is lost, so the next launch starts with volume = 0.
>
> **Workaround**: Manually adjust the master volume in Minecraft’s sound settings once after relaunching.

## **Efficiency & Customization**

- **Auto Attack**: Default interval = 20 ticks (1 second).
- **Auto Planting**: Default interval = 5 ticks (0.25 seconds).

Both intervals are fully adjustable via the **F10 configuration menu**, where you can also:
- Enable **randomized delays** or **skip cycles** to reduce predictability,
- Toggle support for neutral/passive mobs,
- Control whether auto-functions trigger linked muting,
- And more—all without restarting the game.

## **Key Advantages**

- ✅ **Event-Driven Logic**: Uses Minecraft’s native interaction system—no fake clicks, no cursor interference. Works even with GUIs open.
- ✅ **True Background Operation**: Runs reliably when minimized or unfocused—ideal for multitasking.
- ✅ **Non-Intrusive & Safe**: Built-in filters prevent unintended actions and never override manual input.
- ✅ **High Compatibility**: No core file modifications. Theoretically compatible with all other mods.  
  ⚠️ Found an issue? Join our QQ Group: **172442817**.

## **Important Note**

While this mod includes anti-detection features (e.g., randomized timing), it remains an automation aid and **may still be flagged by certain server anti-cheat systems**. Always check with your server administrator before use to avoid potential bans.

## **License**

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.