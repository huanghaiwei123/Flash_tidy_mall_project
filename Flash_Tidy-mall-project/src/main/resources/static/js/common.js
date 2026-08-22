/**
 * 潮汐商城 公共方法
 * - Toast 提示
 * - 导航栏渲染
 * - 用户状态检查
 */

// ====== Toast 提示 ======
function toast(msg, type) {
  type = type || 'success';
  var el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.className = 'toast ' + type + ' show';
  clearTimeout(el._tid);
  el._tid = setTimeout(function () { el.classList.remove('show'); }, 2500);
}

// ====== 用户状态 ======
function isLogin() { return !!localStorage.getItem('token'); }

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('userInfo');
  window.location.href = 'http://localhost:8080/index.html';
}

function getUserId() {
  var uid = localStorage.getItem('userId');
  if (!uid) {
    // 兜底：从 userInfo 中读取
    var info = localStorage.getItem('userInfo');
    if (info) {
      try { uid = JSON.parse(info).userId; } catch(e) {}
    }
  }
  return uid;
}

// ====== 渲染导航栏（每个页面自动调用） ======
function renderNavbar() {
  var nav = document.getElementById('navbar');
  if (!nav) return;
  var logged = isLogin();
  nav.innerHTML =
    '<div class="fm-header">' +
    '  <div class="fm-header-inner">' +
    '    <a href="http://localhost:8080/index.html" class="fm-logo">' +
    '      <img src="http://localhost:8080/img/logo.png" alt="潮汐商城">' +
    '    </a>' +
    '    <div class="fm-search">' +
    '      <input type="text" id="globalKeyword" placeholder="搜索商品名称或品牌..." onkeyup="if(event.key===\'Enter\')globalSearch()">' +
    '      <button class="fm-search-btn" onclick="globalSearch()">搜索</button>' +
    '    </div>' +
    '    <div class="fm-header-nav">' +
    '      <a href="http://localhost:8080/index.html">🏠 首页</a>' +
      '      <a href="http://localhost:8080/seckill.html" style="color:#ff4d2e;font-weight:600;">⚡ 秒杀</a>' +
    (logged
      ? '      <a href="http://localhost:8080/order-list.html">我的订单</a>' +
        '      <a href="http://localhost:8080/user-center.html">个人中心</a>' +
        '      <a href="javascript:void(0)" onclick="changeRolePage()">🏪 商家中心</a>' +
        '      <a href="javascript:void(0)" onclick="changeAdminPage()">⚙️ 管理</a>' +
        '      <button class="btn-logout" onclick="logout()">退出</button>'
      : '      <a href="http://localhost:8080/login.html">你好，请登录</a>' +
        '      <a href="http://localhost:8080/register.html">免费注册</a>') +
    '    </div>' +
    '    <a href="http://localhost:8080/cart.html" class="fm-cart">🛒 购物车<span class="cart-badge" id="cartBadge" style="display:none">0</span></a>' +
    '  </div>' +
    '</div>';
}

// ====== 全局搜索：跳转首页并带上关键词 ======
function globalSearch() {
  var kw = (document.getElementById('globalKeyword') || {}).value;
  if (!kw) { kw = ''; }
  window.location.href = 'http://localhost:8080/index.html?keyword=' + encodeURIComponent(kw.trim());
}

// ====== JWT 过期自动跳转登录 ======
function fetchWithAuth(url, options) {
  options = options || {};
  var token = localStorage.getItem('token');
  if (!options.headers) { options.headers = {}; }
  if (token) { options.headers['Authorization'] = 'Bearer ' + token; }
  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  return fetch(url, options).then(function(res) {
    // HTTP 401 → 自动跳登录
    if (res.status === 401) {
      localStorage.removeItem('token');
      window.location.href = 'http://localhost:8080/login.html';
      return Promise.reject(new Error('未登录或登录已过期'));
    }
    return res.json();
  });
}

// ====== 管理员角色检查 ======
function changeAdminPage() {
  if (!isLogin()) {
    window.location.href = 'http://localhost:8080/login.html';
    return;
  }
  fetchWithAuth('http://localhost:8080/hhw/user/changeAdminPage', { method: 'POST' })
  .then(function(data) {
    if (data.code === 200) {
      window.location.href = 'http://localhost:8080/admin.html';
    } else {
      toast('您不是管理员，无法访问管理后台', 'warning');
    }
  })
  .catch(function() {
    toast('网络异常，请稍后重试', 'warning');
  });
}

// ====== 商家角色检查 + 申请弹窗 ======
function changeRolePage() {
  if (!isLogin()) {
    window.location.href = 'http://localhost:8080/login.html';
    return;
  }
  fetchWithAuth('http://localhost:8080/hhw/user/changePage', { method: 'POST' })
  .then(function(data) {
    if (data.code === 200) {
      // 是商家 → 跳转
      window.location.href = 'http://localhost:8080/merchant.html';
    } else {
      // 不是商家 → 弹窗
      showMerchantApplyModal();
    }
  })
  .catch(function() {
    toast('网络异常，请稍后重试', 'warning');
  });
}

function showMerchantApplyModal() {
  // 移除旧弹窗
  var old = document.getElementById('merchant-apply-modal');
  if (old) { old.parentNode.removeChild(old); }

  var modal = document.createElement('div');
  modal.id = 'merchant-apply-modal';
  modal.className = 'modal';
  modal.style.display = 'flex';
  modal.innerHTML =
    '<div class="modal-overlay" onclick="closeMerchantApplyModal()"></div>' +
    '<div class="modal-box">' +
    '  <h3>🏪 申请成为商家</h3>' +
    '  <form id="merchantApplyForm" onsubmit="submitMerchantApply(event)">' +
    '    <div class="form-group">' +
    '      <label>店铺名称 <span style="color:red">*</span></label>' +
    '      <input type="text" id="applyShopName" placeholder="请输入店铺名称" maxlength="64" required>' +
    '    </div>' +
    '    <div class="form-group">' +
    '      <label>店铺简介</label>' +
    '      <textarea id="applyShopDesc" placeholder="简单介绍一下你的店铺（选填）" maxlength="255" rows="3" style="width:100%;resize:vertical"></textarea>' +
    '    </div>' +
    '    <div class="modal-actions">' +
    '      <button type="button" class="btn-cancel" onclick="closeMerchantApplyModal()">取消</button>' +
    '      <button type="submit" class="btn-primary" id="applySubmitBtn">提交申请</button>' +
    '    </div>' +
    '  </form>' +
    '</div>';
  document.body.appendChild(modal);
}

function closeMerchantApplyModal() {
  var modal = document.getElementById('merchant-apply-modal');
  if (modal) { modal.style.display = 'none'; }
}

function submitMerchantApply(e) {
  e.preventDefault();
  var btn = document.getElementById('applySubmitBtn');
  btn.disabled = true;
  btn.textContent = '提交中...';

  var body = {
    shopName: document.getElementById('applyShopName').value.trim(),
    shopDescription: document.getElementById('applyShopDesc').value.trim()
  };

  fetchWithAuth('http://localhost:8080/hhw/user/applyMerchant', {
    method: 'POST',
    body: body
  })
  .then(function(data) {
    if (data.code === 200) {
      toast(data.msg || '申请已提交，请等待审核');
      closeMerchantApplyModal();
    } else {
      toast(data.msg || '提交失败', 'warning');
      btn.disabled = false;
      btn.textContent = '提交申请';
    }
  })
  .catch(function() {
    toast('网络异常，请稍后重试', 'warning');
    btn.disabled = false;
    btn.textContent = '提交申请';
  });
}

// ====== 首页 AI 客服悬浮入口（中间偏右，4cm×4cm，仅首页显示） ======
function renderAssistantFloat() {
  if (document.getElementById('assistantFloat')) return;
  // 只在首页显示悬浮客服
  var path = window.location.pathname;
  if (path !== '/' && path !== '/index.html' && !path.endsWith('/index.html')) {
    return;
  }
  var div = document.createElement('div');
  div.id = 'assistantFloat';
  div.innerHTML =
    '<div class="assistant-float" onclick="window.location.href=\'http://localhost:8080/assistant.html\'" title="AI 智能客服助手">' +
    '  <video class="assistant-float-video" src="http://localhost:8080/img/products/assistant.mp4" muted loop autoplay playsinline></video>' +
    '  <span class="assistant-float-tag">AI 助手</span>' +
    '</div>';
  document.body.appendChild(div);
}

// ====== 页面初始化 ======
document.addEventListener('DOMContentLoaded', function () {
  renderNavbar();
  renderAssistantFloat();
  // 需要登录的页面检查
  var needAuth = document.body.dataset.auth === 'true';
  if (needAuth && !isLogin()) {
    window.location.href = 'http://localhost:8080/login.html';
  }
});
