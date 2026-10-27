// 晓镜书阁 PC 端 —— 扫码查询页逻辑
// 接口：POST /api/scan/resolve  body {code}  -> {bizType, entity, extra}
//       GET  /api/mobile/stock/quick?keyword=  -> 图书 + 库存 + 货架位
//       GET  /api/qrcode/book/{isbn}（二维码图片）
(function () {
  if (!SHG.initShell()) return;

  const codeEl = document.getElementById('code');
  const resultCard = document.getElementById('resultCard');
  const stockCard = document.getElementById('stockCard');
  const bookTable = document.getElementById('bookTable');
  const stockBody = document.getElementById('stockBody');
  const qrImg = document.getElementById('qrImg');
  const bizTypeEl = document.getElementById('bizType');

  // 从扫码内容中提取 ISBN
  function extractIsbn(code) {
    if (!code) return '';
    code = code.trim();
    if (code.indexOf('SHG:BOOK:') === 0) return code.substring('SHG:BOOK:'.length);
    if (code.indexOf('SHG:STOCK:') === 0) {
      const parts = code.split(':'); // SHG:STOCK:<warehouseId>:<isbn>
      return parts.length >= 4 ? parts[3] : '';
    }
    if (code.indexOf('SHG:CHECK:') === 0) {
      const parts = code.split(':'); // SHG:CHECK:<checkNo>:<isbn>
      return parts.length >= 4 ? parts[3] : '';
    }
    return code; // 视为原始 ISBN
  }

  function renderBook(entity) {
    entity = entity || {};
    const rows = [
      ['ISBN', entity.isbn],
      ['书名', entity.bookName],
      ['作者', entity.author],
      ['出版社', entity.publisherName],
      ['分类', entity.categoryName],
      ['定价', entity.price !== undefined ? '¥' + SHG.fmtMoney(entity.price) : ''],
      ['零售价', entity.salePrice !== undefined ? '¥' + SHG.fmtMoney(entity.salePrice) : ''],
      ['货架位', entity.shelfNo]
    ];
    bookTable.innerHTML = rows.map((r) =>
      '<tr><th style="width:90px">' + r[0] + '</th><td>' + SHG.esc(r[1] || '-') + '</td></tr>'
    ).join('');
    const isbn = entity.isbn || '';
    if (isbn) {
      qrImg.style.display = '';
      qrImg.src = SHG.BASE_URL + '/qrcode/book/' + encodeURIComponent(isbn);
    } else {
      qrImg.style.display = 'none';
    }
  }

  function renderStock(list) {
    list = list || [];
    if (!list.length) {
      stockBody.innerHTML = '<tr><td colspan="4" class="center muted">无库存记录</td></tr>';
      stockCard.style.display = '';
      return;
    }
    stockBody.innerHTML = list.map((s) => {
      const low = Number(s.quantity) < Number(s.safetyStock || 0);
      return '<tr>' +
        '<td>' + SHG.esc(s.warehouseName || s.warehouseId || '-') + '</td>' +
        '<td class="' + (low ? 'diff' : '') + '">' + SHG.esc(s.quantity) + (low ? ' ⚠' : '') + '</td>' +
        '<td>' + SHG.esc(s.safetyStock || 0) + '</td>' +
        '<td>' + SHG.esc(s.shelfNo || '-') + '</td>' +
      '</tr>';
    }).join('');
    stockCard.style.display = '';
  }

  async function doQuery() {
    const raw = (codeEl.value || '').trim();
    if (!raw) return;

    // 1) 解析扫码内容
    let resolved = null;
    try {
      resolved = await SHG.request('/scan/resolve', { method: 'POST', data: { code: raw } });
    } catch (e) {
      // 解析失败不阻断，改用本地解析
      resolved = null;
    }

    const bizType = (resolved && resolved.bizType) || 'BOOK';
    bizTypeEl.textContent = '类型：' + bizType;

    const isbn = extractIsbn(raw);

    // 2) 查询图书 + 实时库存（移动端快捷查询）
    try {
      const quick = await SHG.request('/mobile/stock/quick', { params: { keyword: isbn || raw } });
      // quick 结构兼容：可能是 {book, stockList} 或直接图书对象
      let entity = (resolved && resolved.entity) || {};
      let stockList = [];
      if (quick) {
        if (quick.book) { entity = quick.book; stockList = quick.stockList || quick.stocks || []; }
        else if (quick.bookName || quick.isbn) { entity = quick; stockList = quick.stockList || []; }
        else if (Array.isArray(quick)) { stockList = quick; }
        if (quick.stockList) stockList = quick.stockList;
      }
      if (!entity.isbn && isbn) entity.isbn = isbn;
      renderBook(entity);
      renderStock(stockList);
      resultCard.style.display = '';
    } catch (e) {
      if (resolved && resolved.entity) {
        renderBook(resolved.entity);
        renderStock([]);
        resultCard.style.display = '';
      } else {
        alert('查询失败：' + e.message);
      }
    }
  }

  document.getElementById('queryBtn').addEventListener('click', doQuery);
  codeEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); doQuery(); }
  });
  document.getElementById('clearBtn').addEventListener('click', () => {
    codeEl.value = '';
    resultCard.style.display = 'none';
    stockCard.style.display = 'none';
    codeEl.focus();
  });

  codeEl.focus();
})();