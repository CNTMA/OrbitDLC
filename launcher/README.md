# OrbitVisuals Launcher

Windows-лаунчер запускает Minecraft 26.3 с Fabric и OrbitVisuals напрямую. При первом запуске он скачивает недостающие библиотеки и ресурсы. Для входа используется локальный ник.

На Windows открой `Start OrbitVisuals.cmd`. Для запуска из терминала используй команды из корня OrbitDLC:

```powershell
cd launcher
npm.cmd ci
npm.cmd start
```

Нужны Node.js и Java 25. Готовые JAR-файлы OrbitVisuals и Fabric API лежат в `launcher/bundled-mods`. Миры, моды и настройки хранятся в отдельной папке `launcher/instance`. Серверы с обязательной проверкой Microsoft-аккаунта требуют авторизованную сессию.
