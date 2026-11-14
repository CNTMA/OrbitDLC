const { app, BrowserWindow, ipcMain, shell } = require('electron');
const fs = require('node:fs/promises');
const { spawn, execFileSync } = require('node:child_process');
const { existsSync } = require('node:fs');
const { prepareFabric, listMods } = require('./fabric.cjs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const dataDirectory = process.env.ORBITVISUALS_DATA || __dirname;
const instance = path.join(dataDirectory, 'instance');
const mods = path.join(instance, 'mods');
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
}

function javaExecutable() {
  const candidates = [
    process.env.JAVA_HOME && path.join(process.env.JAVA_HOME, 'bin', 'java.exe'),
    'C:\\Program Files\\Java\\jdk-25\\bin\\java.exe',
    'java.exe'
  ].filter(Boolean);
  for (const java of new Set(candidates)) {
    if (path.isAbsolute(java) && !existsSync(java)) continue;
    try {
      const version = execFileSync(java, ['--version'], { encoding: 'utf8', windowsHide: true, timeout: 5000 });
      if (/^(?:java|openjdk) 25(?:[.\s+-]|$)/m.test(version)) return java;
    } catch { /* Try the next installed runtime. */ }
  }
  throw new Error('Для Minecraft 26.3 установи Java 25 и укажи её в JAVA_HOME');
}

async function launch(username) {
  if (game) return;
  game = true;
  publish('starting', 'Подготовка Fabric…');
  output = '';
  try {
    const java = javaExecutable();
    const cache = process.env.APPDATA && path.join(process.env.APPDATA, '.minecraft');
    const { args } = await prepareFabric(__dirname, cache, username,
      progress => publish('starting', progress.trim()), instance);
    const argumentFile = path.join(instance, 'launch.args');
    await fs.writeFile(argumentFile, args.map(arg => '"' + arg.replaceAll('\\', '\\\\').replaceAll('"', '\\"') + '"').join('\n'));
    log('Запуск Minecraft 26.3 · Fabric…\n');
    const code = await new Promise((resolve, reject) => {
      const client = spawn(java, ['@' + argumentFile], {
        cwd: instance, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true
      });
      client.stdout.setEncoding('utf8').on('data', chunk => {
        log(chunk);
        if (status.kind === 'starting' && /Sound engine started|Created:.*textures\/atlas/.test(output)) {
          publish('running', 'Minecraft запущен');
        }
      });
      client.stderr.setEncoding('utf8').on('data', log);
      client.once('error', reject);
      client.once('close', resolve);
    });
    if (code !== 0) throw new Error(`Minecraft завершился с кодом ${code}`);
    publish('ready', 'Игра закрыта');
  } catch (error) {
    publish('error', error.message);
  } finally {
    game = false;
  }
}

function handle(channel, action) {
  ipcMain.handle(channel, (event, ...args) => {
    if (window && event.sender === window.webContents) return action(...args);
  });
}

async function openFolder(folder) {
  await fs.mkdir(folder, { recursive: true });
  const error = await shell.openPath(folder);
  if (error) throw new Error(error);
}

app.setPath('userData', path.join(dataDirectory, 'app-data'));
app.whenReady().then(() => {
  window = new BrowserWindow({
    width: 1000,
    height: 650,
    minWidth: 710,
    minHeight: 540,
    title: 'OrbitVisuals',
    frame: false,
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
  handle('orbit:launch', launch);
  handle('orbit:window', (action) => {
    if (action === 'close') window.close();
    if (action === 'minimize') window.minimize();
    if (action === 'maximize') {
      if (window.isMaximized()) window.unmaximize();
      else window.maximize();
    }
  });
  handle('orbit:folder', () => openFolder(instance));
  handle('orbit:mods-folder', () => openFolder(mods));
  handle('orbit:mods', () => listMods(__dirname, mods));
  window.loadFile(path.join(__dirname, 'index.html'));
});
app.on('window-all-closed', () => app.quit());
