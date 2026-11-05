JaxClient - Fabric 1.21.4 client MOD (this zip is SOURCE CODE, not a resource pack)

You need a built .jar to use it. Pick ONE way to get it:

A) No Java needed - let GitHub build it (about 5 minutes)
   1. Make a free account at github.com and create a new repository.
   2. Upload everything in this folder, INCLUDING the hidden ".github" folder.
   3. Open the repo's "Actions" tab and wait for the "build" run to turn green.
   4. Open the repo's "Releases" page and download jaxclient-1.0.0.jar. That is the mod.

B) Double-click build (needs JDK 21 or newer installed)
   Windows:     double-click BUILD.bat
   macOS/Linux: run ./build.sh
   The jar appears in the JAR-READY folder.

C) Command line
   ./gradlew build        (gradlew.bat build on Windows)
   Jar: build/libs/jaxclient-1.0.0.jar   (NOT the -sources jar)

INSTALL
   Your profile/instance must be Fabric on Minecraft 1.21.4.
   Drag these two files into its mods folder (or your launcher's mod profile):
     1. jaxclient-1.0.0.jar
     2. Fabric API for 1.21.4 (0.119.4+1.21.4)
   To confirm it loaded, open logs/latest.log and search for "JaxClient loaded".

KEYS (set them in Minecraft: Options > Controls > Key Binds > JaxClient)
   Right Shift  open/close the menu
   B            Fullbright
   V            AutoSprint
   C (hold)     Zoom
   G            Flight        (Friends tab, server-locked)
   H            Player ESP    (Friends tab, server-locked)

MENU
   Render:   Fullbright, Zoom, Low Health Warning
   Movement: AutoSprint
   Utility:  FPS, Ping, Clock, Memory, Coordinates, Keystrokes (+CPS), Keybind List,
             Armor HUD, Potion Effects, Weather Notifier, Biome, Direction, Speedometer,
             World Time, Coordinate Details
   Friends:  Flight, Speed, Player ESP - ONLY work in singleplayer or on servers you allow.
   Settings: accent color, HUD on/off, notifications on/off, zoom FOV slider,
             Server Lock, reset.
   Drag the panel by its top edge. Mouse wheel scrolls long lists.
   Settings and module states save to config/jaxclient.json.

SERVER LOCK (Friends tab)
   Flight, Speed and Player ESP refuse to run on any multiplayer server you have not
   allowed. To allow your friends' server: join it, open the menu (Right Shift),
   go to Settings > Server Lock > "Allow here". Only do this on servers whose owner
   and players have agreed to modified clients. On public servers these features
   can get your account banned and ruin other players' game - leave them locked.
   The allowed list is stored in config/jaxclient.json. It is a guard against
   accidents, not a security boundary: you control that file.

New HUD additions: Biome shows your current biome, Direction shows cardinal facing and yaw,
Speedometer reports horizontal blocks per second, World Time adds the in-game clock to the
status strip, and Coordinate Details shows the current chunk. Toggle them in Utility.

Everything outside the Friends tab (HUD, zoom, fullbright, keystrokes, ...) is
cosmetic or quality-of-life. Check each server's rules before joining.

Added in this build:
- Ore Xray: scans already-loaded chunks within 24 blocks and lists up to 8 nearest ore coordinates in the HUD.
- Chest ESP: scans already-loaded chunks within 24 blocks and lists up to 8 nearest chests, ender chests, barrels, and shulker boxes.
- Both modules are in the Render category and are protected by Server Lock; they work in singleplayer or on explicitly allowed multiplayer servers.
- Scans refresh every 10 client ticks. These are coordinate-scanner overlays, not a block-transparency renderer or 3D bounding-box ESP.


NEW FEATURES
------------
Chest ESP now draws blue debug outlines for detected containers using the debug line render layer.
Auto Tool selects the fastest mining-speed item in the hotbar for the block under the crosshair.

Custom Xray block commands (client-side):
  /jaxxray add minecraft:spawner
  /jaxxray remove minecraft:spawner
  /jaxxray list
  /jaxxray clear

Custom Xray block selections are saved in config/jaxclient.json. Built-in ore targets remain enabled.
