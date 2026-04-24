# 数启欣官网 — 用户注册/登录系统设计

## 概述

为数启欣官网添加用户注册和登录功能。前端延续现有深色科技风设计，后端使用 Java + Spring Boot 部署在阿里云服务器，MySQL 8 存储用户数据。

## 前端设计

### 布局方案：居中卡片 + 独立页面

- **登录页面** (`login.html`)：独立页面，全屏深色背景 + 网格纹理 + 径向渐变光晕，毛玻璃卡片居中
- **注册页面** (`register.html`)：独立页面，与登录页面风格完全一致
- 两个页面通过底部链接互相跳转（"还没有账号？立即注册" / "已有账号？立即登录"）

### 表单风格

- 输入框采用**底线风格**（与首页 Contact 区域一致）：无边框，仅底部 1px 分割线
- 标签使用 `mono` 字体 + 大写英文 + 小号字体（如 `手机号 PHONE`），与现有网站风格统一
- 聚焦时底线变为 accent 色 (`#4DD4AC`)，标签文字同步变色
- 错误提示显示在标签右侧，红色文字

### 注册页面字段

| 字段 | 类型 | 验证规则 |
|------|------|----------|
| 手机号 | text | 必填，11 位，`/^1[3-9]\d{9}$/` |
| 密码 | password | 必填，至少 8 位，包含字母和数字 |
| 确认密码 | password | 必填，与密码一致 |

### 登录页面字段

| 字段 | 类型 | 验证规则 |
|------|------|----------|
| 手机号 | text | 必填，格式校验 |
| 密码 | password | 必填 |

### 卡片内容结构

1. Logo 图标（复用首页 LogoA 组件）
2. 标题（"创建账号" / "欢迎回来"）
3. 副标题（"加入数启欣，开启智能之旅" / "登录你的数启欣账号"）
4. 表单字段
5. 提交按钮（accent 色实心圆角按钮）
6. 底部切换链接

### 登录后导航栏变化

- 导航栏右侧「开始合作 →」按钮替换为：用户手机号后四位显示（如 `用户 *0816`）+ 退出按钮
- 退出后恢复为原始「开始合作」按钮

### 页面路由

- 首页 `/index.html` — 原有网站
- 登录 `/login.html` — 独立页面
- 注册 `/register.html` — 独立页面
- 首页导航栏「开始合作」按钮改为跳转至 `/login.html`

## 后端设计

### 技术栈

- **框架**：Java 17 + Spring Boot 3
- **数据库**：MySQL 8（阿里云 8.130.166.83:3306）
- **认证**：JWT（JSON Web Token），token 存在前端 localStorage
- **密码**：BCrypt 加密存储

### API 设计

#### POST `/api/auth/register`

注册新用户。

**请求体：**
```json
{
  "phone": "13800138000",
  "password": "Abc12345"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGci...",
    "phone": "13800138000"
  }
}
```

**失败响应 (400)：**
```json
{
  "code": 400,
  "message": "该手机号已注册"
}
```

#### POST `/api/auth/login`

用户登录。

**请求体：**
```json
{
  "phone": "13800138000",
  "password": "Abc12345"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGci...",
    "phone": "13800138000"
  }
}
```

**失败响应 (401)：**
```json
{
  "code": 401,
  "message": "手机号或密码错误"
}
```

#### GET `/api/auth/me`

验证当前 token，返回用户信息。Header 携带 `Authorization: Bearer <token>`。

**成功响应 (200)：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "phone": "13800138000",
    "createdAt": "2026-04-25T10:00:00"
  }
}
```

**失败响应 (401)：**
```json
{
  "code": 401,
  "message": "token 无效或已过期"
}
```

### 数据库设计

```sql
CREATE DATABASE IF NOT EXISTS shuqixin DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(11) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### JWT 策略

- 签名算法：HS256
- 有效期：7 天
- Payload：`{ "userId": 1, "phone": "138****8000" }`
- 存储：前端 `localStorage.setItem('token', ...)`

### CORS 配置

后端允许前端域名（Cloudflare Pages 域名）跨域请求，配置 `Access-Control-Allow-Origin`、`Access-Control-Allow-Headers` 等。

## 部署架构

```
用户浏览器
    │
    ├── 静态页面请求 ──→ Cloudflare Pages (index/login/register.html)
    │
    └── API 请求 ──→ 阿里云 8.130.166.83
                      └── Spring Boot (:8080)
                            └── MySQL (:3306)
```

- 前端静态文件继续托管在 Cloudflare Pages
- 后端 Spring Boot 部署在阿里云服务器，监听 8080 端口
- 前端 API 请求地址为 `http://8.130.166.83:8080/api/auth/*`

## 安全考量

- 密码 BCrypt 加密，不存储明文
- JWT 设置合理过期时间（7 天）
- 登录失败不提示具体是手机号还是密码错误（统一提示"手机号或密码错误"）
- 后端对注册/登录接口做基础的请求频率限制
- 前端表单做客户端校验，后端同步校验

## 不包含的功能

以下功能不在本次范围内：
- 短信验证码
- 微信/第三方登录
- 忘记密码/重置密码
- 用户中心/个人资料编辑
- 管理后台
