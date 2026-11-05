JaxClient - Fabric 1.21.4 client MOD (not a resource pack)

This zip is SOURCE CODE. Build it into a jar first, then put the jar in mods.

Build (needs JDK 21 or newer and internet access):
  Windows:     gradlew.bat build
  macOS/Linux: ./gradlew build

Output: build/libs/jaxclient-1.0.0.jar   (not the -sources jar)

Install: put that jar and Fabric API 0.119.4+1.21.4 in .minecraft/mods
(Fabric Loader 0.16+ for Minecraft 1.21.4).

No JDK? Push this folder to a GitHub repo; the included workflow builds the jar
under Actions > Artifacts.

Keys (set them in Minecraft: Options > Controls > Key Binds > JaxClient):
  Right Shift  open/close the menu
  B            Fullbright
  V            AutoSprint

Menu: Render / Movement / Utility tabs with animated toggles, Settings tab with
accent colour (Purple, Cyan, Crimson, Ice), HUD and notification switches.
Drag the panel by its top edge. Scroll with the mouse wheel.

HUD (all toggled in the Utility tab):
  Keybind List (left), Coordinates (top-left), Keystrokes (bottom-left),
  Clock / FPS / Ping strip (bottom-right), enabled module list (top-right),
  two-line slide-in toasts (top-right). Settings save to config/jaxclient.json.


Maintenance fixes in this revision:
  - AutoSprint now releases its synthetic sprint input when forward is released
    or sneak is held, instead of leaving sprint latched.
  - Config loading preserves HUD/notification defaults for older partial configs.
  - Config saves use a temporary file and replace the destination atomically
    where the filesystem supports it.


JaxClient 1.1.0 additions:
  - Armor Status HUD: displays equipped armor and remaining durability.
  - Direction HUD: displays the compass direction you are facing.
  Both are toggled from the Utility tab and saved in the client config.
