// 晓镜书阁 PC 端 —— 通用 API 封装与页面公共逻辑
// 契约依据：01-docs/09-接口设计.md，基础路径 /api。
(function () {
  const BASE_URL = (window.appConfig && window.appConfig.apiBase) || 'http://localhost:8080/api';
  const TOKEN_KEY = 'shg_token';
  const USER_KEY = 'shg_user';

  function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }
  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch (e) { return null; }
  }
  function setAuth(token, userInfo) {
    localStorage.setItem(TOKEN_KEY, token || '');
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo || null));
  }
  function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  // 未登录则跳转登录页
  function requireLogin() {
    if (!getToken()) {
      location.href = 'login.html';
      return false;
    }
    return true;
  }

  function toQuery(params) {
    if (!params) return '';
    const usp = new URLSearchParams();
    Object.keys(params).forEach((k) => {
      const v = params[k];
      if (v !== undefined && v !== null && v !== '') usp.append(k, v);
    });
    const s = usp.toString();
    return s ? ('?' + s) : '';
  }

  /**
   * 统一请求方法
   * @param {string} path  以 /api 之后的路径，如 '/auth/login'
   * @param {object} opt   { method, data, params, raw }
   * 成功返回 data；失败抛出 Error（含 code）
   */
  async function request(path, opt) {
    opt = opt || {};
    const url = BASE_URL + path + toQuery(opt.params);
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    let res;
    try {
      res = await fetch(url, {
        method: opt.method || 'GET',
        headers: headers,
        body: opt.data ? JSON.stringify(opt.data) : undefined
      });
    } catch (e) {
      throw new Error('无法连接后端服务，请确认 ' + BASE_URL + ' 已启动');
    }

    if (opt.raw) return res;

    if (res.status === 401) {
      clearAuth();
      location.href = 'login.html';
      throw new Error('未登录或登录已过期');
    }
    if (res.status === 403) throw new Error('无访问权限');
    if (res.status === 500) throw new Error('系统异常');

    let json;
    try { json = await res.json(); } catch (e) { throw new Error('响应解析失败'); }
    if (json.code !== 0) {
      const err = new Error(json.message || '请求失败');
      err.code = json.code;
      throw err;
    }
    return json.data;
  }

  // 分页响应兼容：返回数组
  function recordsOf(data) {
    if (!data) return [];
    if (Array.isArray(data)) return data;
    return data.records || [];
  }

  function fmtMoney(v) {
    if (v === undefined || v === null || v === '') return '0.00';
    return Number(v).toFixed(2);
  }

  function fmtTime(v) {
    return v ? String(v).replace('T', ' ').substring(0, 19) : '-';
  }

  function esc(s) {
    if (s === undefined || s === null) return '';
    return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  function currentPath() {
    const p = location.pathname.split('/').pop();
    return p || 'index.html';
  }

  // 注入公共左侧导航
  function mountSidebar() {
    const el = document.getElementById('sidebar');
    if (!el) return;
    const active = currentPath();
    const links = [
      { href: 'index.html', icon: '🏠', text: '首页工作台' },
      { href: 'check.html', icon: '🧾', text: '库存盘点' },
      { href: 'scan.html', icon: '📷', text: '扫码查询' },
      { href: 'query.html', icon: '📊', text: '单据 / 统计' }
    ];
    el.innerHTML =
      '<div class="brand"><span class="logo">书</span>晓镜书阁</div>' +
      '<nav>' + links.map((l) =>
        '<a class="' + (active === l.href ? 'active' : '') + '" href="' + l.href + '">' +
        '<span>' + l.icon + '</span>' + l.text + '</a>').join('') + '</nav>' +
      '<div class="side-foot">进销存管理系统 · PC 端<br>信创友好 · Electron</div>';
  }

  // 注入右上角用户信息 + 退出
  function mountUserBox() {
    const el = document.getElementById('userBox');
    if (!el) return;
    const u = getUser();
    const name = u && (u.realName || u.username) ? (u.realName || u.username) : '未知用户';
    el.innerHTML = '<span>👤 ' + esc(name) + '</span>' +
      '<button class="btn sm" id="logoutBtn">退出登录</button>';
    const btn = document.getElementById('logoutBtn');
    if (btn) btn.addEventListener('click', async () => {
      try { await request('/auth/logout', { method: 'POST' }); } catch (e) { /* 忽略 */ }
      clearAuth();
      location.href = 'login.html';
    });
  }

  // 页面初始化脚手架
  function initShell() {
    if (!requireLogin()) return false;
    mountSidebar();
    mountUserBox();
    return true;
  }

  window.SHG = {
    BASE_URL,
    getToken, getUser, setAuth, clearAuth, requireLogin,
    request, recordsOf, fmtMoney, fmtTime, esc,
    initShell, mountUserBox
  };
})();