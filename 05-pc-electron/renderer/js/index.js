// 晓镜书阁 PC 端 —— 主界面逻辑
(function () {
  if (!SHG.initShell()) return;
  document.getElementById('apiBase').textContent = SHG.BASE_URL;

  const KPI_MAP = [
    { key: 'todaySaleAmount', label: '今日销售额', money: true },
    { key: 'todaySaleCount', label: '今日销售单数' },
    { key: 'monthSaleAmount', label: '本月销售额', money: true },
    { key: 'stockAmount', label: '库存总额', money: true },
    { key: 'warningCount', label: '库存预警数' },
    { key: 'pendingCount', label: '待处理单据' }
  ];

  function renderKpi(data) {
    data = data || {};
    const grid = document.getElementById('kpiGrid');
    grid.innerHTML = KPI_MAP.map((k) => {
      let v = data[k.key];
      if (v === undefined || v === null) v = 0;
      if (k.money) v = SHG.fmtMoney(v);
      return '<div class="kpi"><div class="k-label">' + k.label +
        '</div><div class="k-value">' + SHG.esc(v) + '</div></div>';
    }).join('');
  }

  // GET /api/stat/overview —— 工作台 KPI
  SHG.request('/stat/overview')
    .then(renderKpi)
    .catch((e) => {
      document.getElementById('kpiGrid').innerHTML =
        '<div class="kpi"><div class="k-label">概览加载失败</div><div class="k-value">-</div>' +
        '<div class="muted mt8">' + SHG.esc(e.message) + '</div></div>';
    });
})();