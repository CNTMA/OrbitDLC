# OrbitVisuals Launcher

Windows-лаунчер для запуска Minecraft 26.3 с OrbitDLC из этого репозитория. Кнопка «Играть» запускает проект через Gradle Wrapper. При первом запуске Gradle может загрузить зависимости и ресурсы игры.

На Windows открой `Start OrbitVisuals.cmd`. Для запуска из терминала используй команды из корня OrbitDLC:

```powershell
cd launcher
npm.cmd ci
npm.cmd start
```

Нужны Node.js и JDK 25. Лаунчер использует `JAVA_HOME`, либо ищет JDK 25 в `C:\Program Files\Java\jdk-25`.
