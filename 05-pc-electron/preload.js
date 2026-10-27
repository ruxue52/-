// 晓镜书阁 PC 端 —— 预加载脚本
// 通过 contextBridge 向渲染层安全暴露只读配置，避免开启 nodeIntegration。
const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('appConfig', {
  appName: '晓镜书阁',
  appEdition: 'PC 端（Electron）',
  // 后端基地址：可由启动时的环境变量 SHG_API_BASE 覆盖
  apiBase: process.env.SHG_API_BASE || 'http://localhost:8080/api'
});