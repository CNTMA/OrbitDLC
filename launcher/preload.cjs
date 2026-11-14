const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('orbit', {
  launch: (username) => ipcRenderer.invoke('orbit:launch', username),
  windowAction: (action) => ipcRenderer.invoke('orbit:window', action),
  openFolder: () => ipcRenderer.invoke('orbit:folder'),
  openModsFolder: () => ipcRenderer.invoke('orbit:mods-folder'),
  mods: () => ipcRenderer.invoke('orbit:mods'),
  onStatus: (callback) => ipcRenderer.on('orbit:status', (_event, status) => callback(status))
});
