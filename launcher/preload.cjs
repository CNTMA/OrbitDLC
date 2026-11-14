const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('orbit', {
  launch: () => ipcRenderer.invoke('orbit:launch'),
  onStatus: (callback) => ipcRenderer.on('orbit:status', (_event, status) => callback(status)),
  onOutput: (callback) => ipcRenderer.on('orbit:output', (_event, output) => callback(output))
});
