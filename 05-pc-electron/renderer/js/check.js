// 晓镜书阁 PC 端 —— 库存盘点页逻辑
// 接口：GET /api/stock/check/orders（列表）
//       GET /api/stock/check/orders/{id}（详情，含 items）
//       POST /api/mobile/check/scan（扫码录入 {checkId, code, realQty}）
//       PUT  /api/stock/check/orders/{id}/items（批量录入 {items:[{itemId,realQty}]}）
//       POST /api/stock/check/orders/{id}/finish（完成盘点）
(function () {
  if (!SHG.initShell()) return;

  let currentId = null;
  let currentItems = [];   // 最近一次加载的明细（含 itemId/isbn/realQty）

  const STATUS = {
    DRAFT: { text: '盘点中', cls: 'draft' },
    FINISHED: { text: '已完成', cls: 'done' },
    CANCELLED: { text: '已作废', cls: 'cancel' }
  };
  const TYPE = { FULL: '全盘', PART: '抽盘', CYCLE: '循环盘' };

  function tag(status) {
    const s = STATUS[status] || { text: status || '-', cls: 'draft' };
    return '<span class="tag ' + s.cls + '">' + s.text + '</span>';
  }

  // ---- 盘点单列表 ----
  async function loadOrders() {
    const body = document.getElementById('orderBody');
    body.innerHTML = '<tr><td colspan="9" class="center muted">加载中…</td></tr>';
    try {
      const data = await SHG.request('/stock/check/orders', { params: { page: 1, size: 50, sort: 'createTime,desc' } });
      const list = SHG.recordsOf(data);
      if (!list.length) {
        body.innerHTML = '<tr><td colspan="9" class="center muted">暂无盘点单</td></tr>';
        return;
      }
      body.innerHTML = list.map((o) => {
        const canEnter = o.status === 'DRAFT';
        return '<tr>' +
          '<td>' + SHG.esc(o.checkNo) + '</td>' +
          '<td>' + SHG.esc(o.warehouseName || o.warehouseId) + '</td>' +
          '<td>' + (TYPE[o.checkType] || o.checkType || '-') + '</td>' +
          '<td>' + tag(o.status) + '</td>' +
          '<td>' + SHG.esc(o.totalItems) + '</td>' +
          '<td>' + SHG.esc(o.diffItems) + '</td>' +
          '<td>' + SHG.esc(o.operatorName || '-') + '</td>' +
          '<td>' + SHG.fmtTime(o.createTime) + '</td>' +
          '<td><button class="btn sm ' + (canEnter ? 'primary' : '') + '" data-id="' + o.id + '" ' +
            (canEnter ? '' : 'disabled') + '>' + (canEnter ? '进入盘点' : '查看') + '</button></td>' +
          '</tr>';
      }).join('');

      body.querySelectorAll('button[data-id]').forEach((b) => {
        b.addEventListener('click', () => openOrder(b.getAttribute('data-id')));
      });
    } catch (e) {
      body.innerHTML = '<tr><td colspan="9" class="center muted">加载失败：' + SHG.esc(e.message) + '</td></tr>';
    }
  }

  // ---- 打开盘点单详情 ----
  async function openOrder(id) {
    currentId = id;
    document.getElementById('detailCard').style.display = '';
    document.getElementById('itemBody').innerHTML = '<tr><td colspan="6" class="center muted">加载中…</td></tr>';
    try {
      const detail = await SHG.request('/stock/check/orders/' + id);
      document.getElementById('detailNo').textContent = (detail.checkNo || '') + '（' + (TYPE[detail.checkType] || detail.checkType || '') + '）';
      renderItems(detail.items || []);
      document.getElementById('scanInput').focus();
    } catch (e) {
      document.getElementById('itemBody').innerHTML = '<tr><td colspan="6" class="center muted">加载失败：' + SHG.esc(e.message) + '</td></tr>';
    }
  }

  function renderItems(items) {
    currentItems = items.map((it) => ({
      itemId: it.id,
      isbn: it.isbn,
      bookName: it.bookName,
      bookQty: Number(it.bookQty || 0),
      realQty: Number(it.realQty || 0),
      shelfNo: it.shelfNo || ''
    }));
    const tb = document.getElementById('itemBody');
    if (!currentItems.length) {
      tb.innerHTML = '<tr><td colspan="6" class="center muted">该盘点单暂无明细</td></tr>';
      updateStat();
      return;
    }
    tb.innerHTML = currentItems.map((it, idx) => {
      const diff = it.realQty - it.bookQty;
      const diffCls = diff === 0 ? 'muted' : 'diff';
      return '<tr>' +
        '<td>' + SHG.esc(it.isbn) + '</td>' +
        '<td>' + SHG.esc(it.bookName) + '</td>' +
        '<td>' + it.bookQty + '</td>' +
        '<td><input type="number" min="0" step="1" data-idx="' + idx + '" value="' + it.realQty + '" style="width:96px;height:32px" /></td>' +
        '<td class="' + diffCls + '">' + (diff > 0 ? '+' + diff : diff) + '</td>' +
        '<td>' + SHG.esc(it.shelfNo || '-') + '</td>' +
        '</tr>';
    }).join('');

    tb.querySelectorAll('input[data-idx]').forEach((inp) => {
      inp.addEventListener('input', () => {
        const i = Number(inp.getAttribute('data-idx'));
        currentItems[i].realQty = Number(inp.value || 0);
        updateStat();
      });
    });
    updateStat();
  }

  function updateStat() {
    const total = currentItems.length;
    const diff = currentItems.filter((it) => it.realQty - it.bookQty !== 0).length;
    document.getElementById('statText').textContent =
      '共 ' + total + ' 个品种，已录入差异 ' + diff + ' 项';
  }

  // ---- 扫码录入（POST /api/mobile/check/scan） ----
  async function doScan() {
    if (!currentId) { alert('请先选择盘点单'); return; }
    const input = document.getElementById('scanInput');
    const code = (input.value || '').trim();
    if (!code) return;
    const realQty = Number(document.getElementById('scanQty').value || 1);
    try {
      await SHG.request('/mobile/check/scan', {
        method: 'POST',
        data: { checkId: Number(currentId), code: code, realQty: realQty }
      });
      input.value = '';
      input.focus();
      // 重新拉取明细以反映扫码结果
      const detail = await SHG.request('/stock/check/orders/' + currentId);
      renderItems(detail.items || []);
    } catch (e) {
      alert('扫码录入失败：' + e.message);
    }
  }

  // ---- 保存批量实盘数量（PUT .../items） ----
  async function saveItems() {
    if (!currentId) return;
    const items = currentItems.map((it) => ({ itemId: it.itemId, realQty: it.realQty }));
    try {
      await SHG.request('/stock/check/orders/' + currentId + '/items', {
        method: 'PUT',
        data: { items: items }
      });
      alert('实盘数量已保存');
      const detail = await SHG.request('/stock/check/orders/' + currentId);
      renderItems(detail.items || []);
    } catch (e) {
      alert('保存失败：' + e.message);
    }
  }

  // ---- 完成盘点 ----
  async function finish() {
    if (!currentId) return;
    if (!confirm('确认完成盘点？系统将按差异调整库存并写入流水。')) return;
    try {
      await SHG.request('/stock/check/orders/' + currentId + '/finish', { method: 'POST' });
      alert('盘点已完成');
      document.getElementById('detailCard').style.display = 'none';
      currentId = null;
      loadOrders();
    } catch (e) {
      alert('完成盘点失败：' + e.message);
    }
  }

  // ---- 事件绑定 ----
  document.getElementById('refreshOrders').addEventListener('click', loadOrders);
  document.getElementById('scanBtn').addEventListener('click', doScan);
  document.getElementById('scanInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); doScan(); }
  });
  document.getElementById('saveItems').addEventListener('click', saveItems);
  document.getElementById('finishBtn').addEventListener('click', finish);

  loadOrders();
})();