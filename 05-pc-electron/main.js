// 晓镜书阁 · 书店进销存管理系统 PC 端 —— Electron 主进程
// 说明：信创 OS（麒麟 / 统信 UOS）上以 AppImage / deb / rpm 分发运行。
const { app, BrowserWindow, Menu, shell } = require('electron');
const path = require('path');

// 允许通过环境变量覆盖后端地址（信创环境常见为内网地址）
process.env.SHG_API_BASE = process.env.SHG_API_BASE || 'http://localhost:8080/api';

let mainWindow = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1360,
    height: 860,
    minWidth: 1024,
    minHeight: 640,
    title: '晓镜书阁 · 书店进销存管理系统',
    backgroundColor: '#f4f6fb',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,   // 安全：渲染层与 Node 隔离
      nodeIntegration: false,
      sandbox: false
    }
  });

  // 首屏加载登录页
  mainWindow.loadFile(path.join(__dirname, 'renderer', 'login.html'));

  // 外部链接用系统浏览器打开，避免在应用内打开
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  // 开发模式（npm run dev）自动打开 DevTools
  if (process.argv.includes('--dev')) {
    mainWindow.webContents.openDevTools();
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// 精简菜单：保留常用项，隐藏默认英文菜单
function buildMenu() {
  const template = [
    {
      label: '文件',
      submenu: [
        { label: '返回主界面', click: () => mainWindow && mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html')) },
        { type: 'separator' },
        { label: '退出', role: 'quit' }
      ]
    },
    {
      label: '视图',
      submenu: [
        { label: '刷新', role: 'reload' },
        { label: '全屏', role: 'togglefullscreen' },
        { label: '开发者工具', role: 'toggleDevTools' }
      ]
    }
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

app.whenReady().then(() => {
  buildMenu();
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});