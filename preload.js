const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    send: (channel, data) => ipcRenderer.send(channel, data),
    invoke: (channel, ...args) => ipcRenderer.invoke(channel, ...args),
    on: (channel, callback) => {
        const newCallback = (event, ...args) => callback(...args);
        ipcRenderer.on(channel, newCallback);
        return () => ipcRenderer.removeListener(channel, newCallback);
    }
});