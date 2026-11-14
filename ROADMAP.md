# OrbitDLC implementation roadmap

1. Reproduce the existing build with JDK 25 and verify the exact pinned dependencies. Keep the current Discord IPC and icon. Wire the client source set into Loom and declare the Fabric client entrypoint.
2. Add a registry with exactly five categories, stable module identifiers, lifecycle callbacks and Fabric event adapters. Start with independently verifiable modules.
3. Add the ClickGUI with search, settings, keybind capture, favorites and profile selection. Verify native background blur and mouse and keyboard behavior in game.
4. Build ShaderSky on the game's rendering abstraction. Validate GLSL resources, transitions and GPU cleanup on OpenGL and experimental Vulkan. Build a bounded particle engine with reusable emitters.
5. Implement the inventory in MODULE_STATUS.md category by category. Prefer manual combat feedback, training and statistics over input automation. Validate movement features against normal gameplay and server rules. Review overlapping concepts before counting them. Require at least 50 independent modules per category.
6. Add the HUD editor and validated JSON profiles with atomic saves, import, export, backup and restore.
7. Exercise each module in game, including disabling, switching worlds and reloading its settings. Record testing status per module.
8. Build the release JAR and smoke-test it in a separate Fabric 26.2 game instance before release.
