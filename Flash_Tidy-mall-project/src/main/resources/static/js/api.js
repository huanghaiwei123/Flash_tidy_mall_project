/**
 * 潮汐商城 API 请求封装
 * - 自动附加 JWT token
 * - 统一错误处理
 * - 支持 GET/POST/PUT/DELETE
 */

const BASE = 'http://localhost:8080/hhw';

/**
 * 发起请求
 * @param {string} method  GET | POST | PUT | DELETE
 * @param {string} url     完整路径，如 /goods/list
 * @param {object} data    请求体（GET 请求自动拼到 query string）
 * @returns {Promise<object>} { code, msg, data }
 */
async function request(method, url, data) {
  const headers = { 'Content-Type': 'application/json' };
  const token = localStorage.getItem('token');
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const config = { method, headers };

  if (data) {
    if (method === 'GET') {
      const params = new URLSearchParams();
      Object.keys(data).forEach(k => {
        if (data[k] !== undefined && data[k] !== null && data[k] !== '') {
          params.append(k, data[k]);
        }
      });
      const qs = params.toString();
      if (qs) url = url + '?' + qs;
    } else {
      config.body = JSON.stringify(data);
    }
  }

  try {
    const res = await fetch(BASE + url, config);
    const json = await res.json();
    if (json.code === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      window.location.href = 'http://localhost:8080/login.html';
      return null;
    }
    return json;
  } catch (err) {
    toast('网络错误，请稍后重试', 'error');
    console.error('API Error:', err);
    return null;
  }
}

/** GET 请求 */
function get(url, data) { return request('GET', url, data); }
/** POST 请求 */
function post(url, data) { return request('POST', url, data); }
/** PUT 请求 */
function put(url, data) { return request('PUT', url, data); }
/** DELETE 请求 */
function del(url, data) { return request('DELETE', url, data); }
