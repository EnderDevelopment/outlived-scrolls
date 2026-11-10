# Minecraft Scroll Abilities Plugin

Outlived Scrolls is a Minecraft plugin for Paper servers that adds collectible ability scrolls, a scroll selection menu, right-click activation, particle and sound effects, and timed cooldowns. Its five scroll types provide combat, movement, healing, and crowd-control abilities.

## Install

1. Use a Paper server running Minecraft 1.21.11 with Java 21.
2. Build the plugin with `mvn package`.
3. Copy `target/plugin.jar` into the server’s `plugins` folder.
4. Restart the server.

## Use

- Run `/scroll` to open the scroll menu. Click a scroll to add it to your inventory.
- Run `/scroll list` to see the available scroll types.
- Right-click a scroll to activate it. Scrolls are consumed on use, and each player has a shared cooldown across all scroll types.
- Operators or players with `riftscrolls.admin` can run `/scroll give <player> <type>` to give a scroll. Types are `ember`, `frost`, `storm`, `vital`, and `void`.

Ember damages and ignites nearby living entities. Frost damages and slows nearby living entities. Storm blinks the player forward through passable blocks. Vital restores health and grants regeneration. Void pulls nearby living entities toward the player.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/ade5e1e0-086a-40f3-b124-6ff72660f307?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=ade5e1e0-086a-40f3-b124-6ff72660f307
