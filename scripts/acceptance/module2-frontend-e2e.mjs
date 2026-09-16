// 模块 2 端到端验收：用真实浏览器跑通 mall-admin-web 的登录与权限管理页面。
//
// 这一步验证的是 04-开发顺序与验收.md 里说的"真正的试金石"：
// 前端能登录进来，说明接口契约完全对齐。
//
// 前置：
//   1. MySQL(3307) / Redis(6379) 已启动，mall 库已导入 sql/auth-schema.sql + sql/auth-seed.sql
//   2. 后端：java -jar mall-admin/target/mall-admin-1.0-SNAPSHOT.jar （8080）
//   3. 前端：在 mall-admin-web 目录执行 npm run dev （5173）
//   4. 本目录依赖：npm i playwright-core（复用系统安装的 Chrome/Edge，不下载浏览器）
//
// 运行：node module2-frontend-e2e.mjs
// 可用环境变量覆盖：MALL_WEB_URL / MALL_API_PORT / CHROME_PATH / MALL_SHOTS_DIR
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';

const CHROME = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const BASE = process.env.MALL_WEB_URL || 'http://localhost:5173';
const API_PORT = process.env.MALL_API_PORT || '8080';
const SHOTS = process.env.MALL_SHOTS_DIR || 'shots';
mkdirSync(SHOTS, { recursive: true });

const results = [];
const api = [];
const pageErrors = [];
const requestFailed = [];
const consoleMessages = [];
let failed = 0;

function check(name, ok, detail = '') {
  if (!ok) failed++;
  results.push(`${ok ? 'PASS' : 'FAIL'}  ${name.padEnd(46)} ${detail}`);
}

const browser = await chromium.launch({ executablePath: CHROME, headless: true });
const context = await browser.newContext({ viewport: { width: 1500, height: 950 } });
const page = await context.newPage();

page.on('response', (response) => {
  const url = new URL(response.url());
  if (url.port === API_PORT) {
    api.push({ method: response.request().method(), path: url.pathname + url.search, status: response.status() });
  }
});
page.on('pageerror', (error) => pageErrors.push(String(error).split('\n')[0]));
page.on('requestfailed', (request) => {
  const url = new URL(request.url());
  if (url.port === API_PORT) {
    requestFailed.push(`${request.method()} ${url.pathname} :: ${request.failure()?.errorText}`);
  }
});
page.on('console', (message) => {
  if (message.type() === 'error' || message.type() === 'warning') {
    consoleMessages.push(`[${message.type()}] ${message.text().slice(0, 200)}`);
  }
});

async function apiCall(pathFragment, method = 'GET') {
  return api.find((call) => call.method === method && call.path.startsWith(pathFragment));
}

try {
  // 1. 打开前端，应被路由守卫送到登录页
  await page.goto(`${BASE}/#/`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('input[name="username"]', { timeout: 90000 });
  check('打开 mall-admin-web 并被送到登录页', page.url().includes('login'), page.url());

  // 2. 用 admin / macro123 登录
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'macro123');
  await page.click('button:has-text("登录")');

  await page.waitForFunction(() => !location.hash.includes('login'), null, { timeout: 60000 });
  check('登录成功并跳离登录页', true, page.url());

  const login = await apiCall('/admin/login', 'POST');
  check('POST /admin/login 返回 200', login?.status === 200, JSON.stringify(login));

  const info = await apiCall('/admin/info');
  check('GET /admin/info 返回 200', info?.status === 200, JSON.stringify(info));

  // 3. 侧边栏应渲染出 /admin/info 返回的菜单
  await page.waitForSelector('.el-menu', { timeout: 60000 });
  const menuTitles = await page.$$eval('.el-menu .el-sub-menu__title, .el-menu .el-menu-item',
    (nodes) => nodes.map((n) => n.textContent.trim()).filter(Boolean));
  check('/admin/info 的 menus 渲染出侧边栏', menuTitles.length > 0,
    `菜单项 ${menuTitles.length} 个: ${menuTitles.slice(0, 6).join(' / ')}`);

  await page.screenshot({ path: `${SHOTS}/1-home.png` });

  // 4. 逐个打开模块 2 的管理页面，验证对应接口真的通了
  const pages = [
    { hash: '/ums/admin', title: '用户管理', apiPath: '/admin/list' },
    { hash: '/ums/role', title: '角色管理', apiPath: '/role/list' },
    { hash: '/ums/menu', title: '菜单管理', apiPath: '/menu/list' },
    { hash: '/ums/resource', title: '资源管理', apiPath: '/resource/list' },
    { hash: '/ums/resourceCategory', title: '资源分类', apiPath: '/resourceCategory/listAll' },
  ];

  for (const [index, target] of pages.entries()) {
    // hash 路由下 goto 只是同文档跳转，旧页面的表格会残留在 DOM 里导致误判；
    // 这里跳转后强制 reload，让目标页面从零渲染，并用 waitForResponse 确认它真的调了接口。
    const responsePromise = page
      .waitForResponse((response) => response.url().includes(target.apiPath), { timeout: 45000 })
      .catch(() => null);

    await page.goto(`${BASE}/#${target.hash}`, { waitUntil: 'domcontentloaded' });
    await page.reload({ waitUntil: 'domcontentloaded' });

    const response = await responsePromise;
    await page.waitForTimeout(1500);

    const rowCount = await page.$$eval('.el-table__row', (nodes) => nodes.length);
    check(`${target.title} 页面加载 ${target.apiPath}`, response?.status() === 200 && rowCount > 0,
      `${target.apiPath} -> ${response?.status()} 表格行数=${rowCount}`);
    await page.screenshot({ path: `${SHOTS}/${index + 2}-${target.hash.split('/').pop()}.png` });
  }

  // 5. 整个过程中不应有失败的接口调用
  const badCalls = api.filter((call) => call.status >= 400);
  check('整个过程没有 4xx/5xx 接口调用', badCalls.length === 0, JSON.stringify(badCalls.slice(0, 5)));
  check('没有失败的网络请求（含 CORS 被拦）', requestFailed.length === 0, requestFailed.slice(0, 3).join(' | '));
  check('页面没有未捕获的 JS 异常', pageErrors.length === 0, pageErrors.slice(0, 3).join(' | '));
} catch (error) {
  failed++;
  results.push(`FAIL  执行异常: ${error.message}`);
  await page.screenshot({ path: `${SHOTS}/error.png` }).catch(() => {});
} finally {
  await browser.close();
}

console.log(results.join('\n'));
console.log('\n--- 浏览器发出的后端调用 ---');
for (const call of api) console.log(`${call.method} ${call.path} -> ${call.status}`);
if (requestFailed.length) {
  console.log('\n--- 失败的后端请求 ---');
  for (const line of requestFailed) console.log(line);
}
if (consoleMessages.length) {
  console.log('\n--- 浏览器控制台 error/warning ---');
  for (const line of consoleMessages.slice(0, 10)) console.log(line);
}
if (pageErrors.length) {
  console.log('\n--- 未捕获 JS 异常 ---');
  for (const line of pageErrors.slice(0, 5)) console.log(line);
}
console.log(`\nRESULT: ${failed === 0 ? 'ALL PASS' : failed + ' FAILED'}`);
process.exit(failed === 0 ? 0 : 1);
