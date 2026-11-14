const { app, BrowserWindow, ipcMain } = require('electron');
const { spawn } = require('node:child_process');
const { existsSync } = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
let game;
let window;
let status = { kind: 'ready', message: 'Готов к запуску' };
let output = '';

function publish(kind, message) {
  status = { kind, message };
  if (window && !window.isDestroyed()) window.webContents.send('orbit:status', status);
}

function log(chunk) {
  output = (output + chunk).slice(-16000);
  if (window && !window.isDestroyed()) window.webContents.send('orbit:output', output);
}

function launch() {
  if (game) return;
  if (!existsSync(path.join(root, 'gradlew.bat'))) {
    publish('error', 'Gradle Wrapper не найден');
    return;
  }
  const env = { ...process.env };
  const bundledLocation = 'C:\\Program Files\\Java\\jdk-25';
  if (!env.JAVA_HOME && existsSync(path.join(bundledLocation, 'bin', 'java.exe'))) {
    env.JAVA_HOME = bundledLocation;
  }
  publish('starting', 'Подготовка Minecraft…');
  output = '';
  log('');
  game = spawn(process.env.ComSpec || 'cmd.exe',
    ['/d', '/c', 'gradlew.bat runClient --console=plain'],
    { cwd: root, env, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  game.stdout.setEncoding('utf8');
  game.stderr.setEncoding('utf8');
  const handleOutput = (chunk) => {
    log(chunk);
    if (status.kind === 'starting' && /Sound engine started|Created:.*textures\/atlas/.test(output)) {
      publish('running', 'Клиент запущен');
    }
  };
  game.stdout.on('data', handleOutput);
  game.stderr.on('data', handleOutput);
  game.once('error', (error) => {
    log(error.message);
    publish('error', 'Не удалось запустить Gradle');
  });
  game.once('close', (code) => {
    game = undefined;
    if (status.kind === 'error') return;
    if (code === 0) publish('ready', 'Игра закрыта');
    else publish('error', 'Minecraft завершился с ошибкой');
  });
}

app.setPath('userData', path.join(root, 'build', 'orbit-launcher'));
app.whenReady().then(() => {
  window = new BrowserWindow({
    width: 1000,
    height: 650,
    minWidth: 710,
    minHeight: 540,
    title: 'OrbitVisuals',
    backgroundColor: '#101f26',
    autoHideMenuBar: true,
    icon: path.join(root, 'src/main/resources/assets/orbitdlc/black_hole.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.once('closed', () => { window = undefined; });
  window.webContents.on('will-navigate', (event) => event.preventDefault());
  ipcMain.handle('orbit:launch', (event) => {
    if (window && event.sender === window.webContents) launch();
  });
  window.loadFile(path.join(__dirname, 'index.html'));
});
app.on('window-all-closed', () => app.quit());
