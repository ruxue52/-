// 晓镜书阁 PC 端 —— 单据 / 统计查询页逻辑
// 接口：GET /api/sale/orders、GET /api/purchase/orders、GET /api/mobile/stat/summary
(function () {
  if (!SHG.initShell()) return;

  const SALE_STATUS = { DRAFT: ['草稿', 'draft'], SUBMITTED: ['已提交', 'warn'], OUTSTOCK: ['已出库', 'done'], CANCELLED: ['已作废', 'cancel'] };
  const PUR_STATUS = { DRAFT: ['草稿', 'draft'], SUBMITTED: ['已提交', 'warn'], INSTOCK: ['已入库', 'done'], CANCELLED: ['已作废', 'cancel'] };
  const PAY_TYPE = { CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', CARD: '银行卡', CREDIT: '赊账' };

  let tab = 'sale';

  function tag(map, s) {
    const v = map[s] || [s || '-', 'draft'];
    return '<span class="tag ' + v[1] + '">' + v[0] + '</span>';
  }

  async function loadSale() {
    const body = document.getElementById('saleBody');
    body.innerHTML = '<tr><td colspan="9" class="center muted">加载中…</td></tr>';
    const keyword = document.getElementById('keyword').value.trim();
    try {
      const data = await SHG.request('/sale/orders', { params: { page: 1, size: 30, sort: 'createTime,desc', keyword: keyword } });
      const list = SHG.recordsOf(data);
      if (!list.length) { body.innerHTML = '<tr><td colspan="9" class="center muted">暂无数据</td></tr>'; return; }
      body.innerHTML = list.map((o) => '<tr>' +
        '<td>' + SHG.esc(o.orderNo) + '</td>' +
        '<td>' + SHG.esc(o.customerName || '散户') + '</td>' +
        '<td>' + SHG.esc(o.warehouseName || o.warehouseId || '-') + '</td>' +
        '<td>' + SHG.esc(o.totalQty) + '</td>' +
        '<td>¥' + SHG.fmtMoney(o.totalAmount) + '</td>' +
        '<td>¥' + SHG.fmtMoney(o.payAmount) + '</td>' +
        '<td>' + (PAY_TYPE[o.paymentType] || o.paymentType || '-') + '</td>' +
        '<td>' + tag(SALE_STATUS, o.status) + '</td>' +
        '<td>' + SHG.fmtTime(o.createTime) + '</td>' +
      '</tr>').join('');
    } catch (e) {
      body.innerHTML = '<tr><td colspan="9" class="center muted">加载失败：' + SHG.esc(e.message) + '</td></tr>';
    }
  }

  async function loadPurchase() {
    const body = document.getElementById('purchaseBody');
    body.innerHTML = '<tr><td colspan="8" class="center muted">加载中…</td></tr>';
    const keyword = document.getElementById('keyword').value.trim();
    try {
      const data = await SHG.request('/purchase/orders', { params: { page: 1, size: 30, sort: 'createTime,desc', keyword: keyword } });
      const list = SHG.recordsOf(data);
      if (!list.length) { body.innerHTML = '<tr><td colspan="8" class="center muted">暂无数据</td></tr>'; return; }
      body.innerHTML = list.map((o) => '<tr>' +
        '<td>' + SHG.esc(o.orderNo) + '</td>' +
        '<td>' + SHG.esc(o.supplierName || o.supplierId || '-') + '</td>' +
        '<td>' + SHG.esc(o.warehouseName || o.warehouseId || '-') + '</td>' +
        '<td>' + SHG.esc(o.totalQty) + '</td>' +
        '<td>¥' + SHG.fmtMoney(o.totalAmount) + '</td>' +
        '<td>¥' + SHG.fmtMoney(o.payAmount) + '</td>' +
        '<td>' + tag(PUR_STATUS, o.status) + '</td>' +
        '<td>' + SHG.fmtTime(o.createTime) + '</td>' +
      '</tr>').join('');
    } catch (e) {
      body.innerHTML = '<tr><td colspan="8" class="center muted">加载失败：' + SHG.esc(e.message) + '</td></tr>';
    }
  }

  async function loadStat() {
    const grid = document.getElementById('statGrid');
    grid.innerHTML = '<div class="kpi"><div class="k-label">加载中…</div><div class="k-value">-</div></div>';
    try {
      const data = await SHG.request('/mobile/stat/summary');
      const items = [];
      Object.keys(data || {}).forEach((k) => items.push([k, data[k]]));
      if (!items.length) { grid.innerHTML = '<div class="kpi"><div class="k-label">暂无统计数据</div></div>'; return; }
      const LABELS = {
        todaySaleAmount: '今日销售额', todaySaleCount: '今日销售单数',
        monthSaleAmount: '本月销售额', stockAmount: '库存总额',
        warningCount: '库存预警数', bookCount: '图书品种数', totalSku: 'SKU 数'
      };
      grid.innerHTML = items.map((kv) => {
        const label = LABELS[kv[0]] || kv[0];
        let val = kv[1];
        if (typeof val === 'number') val = val;
        return '<div class="kpi"><div class="k-label">' + SHG.esc(label) +
          '</div><div class="k-value">' + SHG.esc(val) + '</div></div>';
      }).join('');
    } catch (e) {
      grid.innerHTML = '<div class="kpi"><div class="k-label">加载失败</div><div class="k-value">-</div><div class="muted mt8">' + SHG.esc(e.message) + '</div></div>';
    }
  }

  function switchTab(next) {
    tab = next;
    document.getElementById('saleCard').style.display = next === 'sale' ? '' : 'none';
    document.getElementById('purchaseCard').style.display = next === 'purchase' ? '' : 'none';
    document.getElementById('statCard').style.display = next === 'stat' ? '' : 'none';
    document.querySelectorAll('button[data-tab]').forEach((b) => {
      b.classList.toggle('primary', b.getAttribute('data-tab') === next);
    });
    if (next === 'sale') loadSale();
    else if (next === 'purchase') loadPurchase();
    else loadStat();
  }

  document.querySelectorAll('button[data-tab]').forEach((b) => {
    b.addEventListener('click', () => switchTab(b.getAttribute('data-tab')));
  });
  document.getElementById('searchBtn').addEventListener('click', () => {
    if (tab === 'sale') loadSale();
    else if (tab === 'purchase') loadPurchase();
    else loadStat();
  });
  document.getElementById('keyword').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') document.getElementById('searchBtn').click();
  });

  switchTab('sale');
})();