const play = document.querySelector('.play');
const status = document.querySelector('.status');
const message = status.querySelector('span');
const username = document.querySelector('#username');
username.value = localStorage.getItem('orbit-username') || 'OrbitPlayer';
const modsButton = document.querySelector('#mods-button');
const modsPanel = document.querySelector('#mods-panel');
const modsList = document.querySelector('#mods-list');
const modsStatus = document.querySelector('#mods-status');

for (const button of document.querySelectorAll('[data-window]')) {
  button.addEventListener('click', () => window.orbit.windowAction(button.dataset.window));
}

async function refreshMods() {
  try {
    const names = await window.orbit.mods();
    modsList.replaceChildren(...names.map(name => {
      const item = document.createElement('li');
      item.textContent = name;
      return item;
    }));
    modsStatus.textContent = names.length ? '' : 'Здесь появятся JAR-файлы из папки mods.';
  } catch {
    modsStatus.textContent = 'Не удалось прочитать папку mods.';
  }
}

modsButton.addEventListener('click', () => {
  modsPanel.hidden = !modsPanel.hidden;
  modsButton.setAttribute('aria-expanded', String(!modsPanel.hidden));
  if (!modsPanel.hidden) refreshMods();
});
document.querySelector('#refresh-mods').addEventListener('click', refreshMods);
document.querySelector('#game-folder').addEventListener('click', async () => {
  try { await window.orbit.openFolder(); }
  catch { message.textContent = 'Не удалось открыть папку игры'; }
});
document.querySelector('#mods-folder').addEventListener('click', async () => {
  try { await window.orbit.openModsFolder(); }
  catch { modsStatus.textContent = 'Не удалось открыть папку mods.'; }
});

function showStatus({ kind, message: text }) {
  status.dataset.kind = kind;
  message.textContent = text;
  play.disabled = kind === 'starting' || kind === 'running';
  if (kind === 'ready' && !modsPanel.hidden) refreshMods();
}

window.orbit.onStatus(showStatus);
play.addEventListener('click', async () => {
  play.disabled = true;
  try {
    const name = username.value.trim();
    if (!/^[a-zA-Z0-9_]{3,16}$/.test(name)) {
      showStatus({ kind: 'error', message: 'Ник: 3–16 букв, цифр или символов _' });
      username.focus();
      return;
    }
    localStorage.setItem('orbit-username', name);
    await window.orbit.launch(name);
  } catch {
    showStatus({ kind: 'error', message: 'Не удалось передать команду запуска' });
  }
});
