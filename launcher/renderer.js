const play = document.querySelector('.play');
const status = document.querySelector('.status');
const message = status.querySelector('span');
const details = document.querySelector('details');
const log = document.querySelector('pre');

function showStatus({ kind, message: text }) {
  status.dataset.kind = kind;
  message.textContent = text;
  play.disabled = kind === 'starting' || kind === 'running';
  if (kind === 'error') details.open = true;
}

window.orbit.onStatus(showStatus);
window.orbit.onOutput((text) => {
  log.textContent = text || 'Ожидание вывода Gradle…';
  log.scrollTop = log.scrollHeight;
});
play.addEventListener('click', async () => {
  play.disabled = true;
  try {
    await window.orbit.launch();
  } catch {
    showStatus({ kind: 'error', message: 'Не удалось передать команду запуска' });
  }
});
