// 晓镜书阁 PC 端 —— 登录页逻辑
(function () {
  const usernameEl = document.getElementById('username');
  const passwordEl = document.getElementById('password');
  const msgEl = document.getElementById('msg');
  const btn = document.getElementById('loginBtn');

  // 已登录直接进入主界面
  if (SHG.getToken()) {
    location.href = 'index.html';
    return;
  }

  function showMsg(text) { msgEl.textContent = text || ''; }

  async function doLogin() {
    const username = (usernameEl.value || '').trim();
    const password = passwordEl.value || '';
    if (!username || !password) { showMsg('请输入账号与密码'); return; }

    btn.disabled = true;
    btn.textContent = '登录中…';
    showMsg('');
    try {
      // POST /api/auth/login  ->  { token, userInfo, permissions }
      const data = await SHG.request('/auth/login', {
        method: 'POST',
        data: { username: username, password: password }
      });
      SHG.setAuth(data.token, data.userInfo);
      location.href = 'index.html';
    } catch (e) {
      showMsg(e.message || '登录失败');
    } finally {
      btn.disabled = false;
      btn.textContent = '登 录';
    }
  }

  btn.addEventListener('click', doLogin);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') doLogin();
  });
  usernameEl.focus();
})();