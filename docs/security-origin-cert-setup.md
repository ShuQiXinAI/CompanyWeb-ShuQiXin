# Cloudflare Origin 证书 + Full (Strict) TLS 迁移手册

本文档描述如何将 `api.shuqixin.com.cn` 的 Cloudflare SSL/TLS 模式从当前的 **Flexible**（明文 HTTP 在 Cloudflare ↔ 源服务器之间传输）升级为 **Full (Strict)**（端到端 HTTPS，Cloudflare 校验源服务器证书）。

同时，完成后将关闭服务器对公网直接暴露的 **8080 端口**。

---

## 背景：为什么要升级

| 模式 | Cloudflare → 源服务器 | 风险 |
|------|----------------------|------|
| Flexible（当前）| HTTP 明文 | 用户 JWT token、密码在阿里云内网段明文传输 |
| Full | HTTPS，但不校验证书 | 允许自签名证书，存在中间人风险 |
| **Full (Strict)** | HTTPS，校验证书 | 推荐生产配置 |

---

## 步骤 1：在 Cloudflare 申请 Origin Certificate

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com) → 选择域名 `shuqixin.com.cn`
2. 左侧菜单 → **SSL/TLS** → **Origin Server** → **Create Certificate**
3. 配置：
   - **Private key type**：RSA (2048)（或 ECC P-256）
   - **Hostnames**：`shuqixin.com.cn`, `*.shuqixin.com.cn`（通配符覆盖 `api.`、`www.` 等所有子域）
   - **Certificate Validity**：15 years（免费，最长选项）
4. 点击 **Create**，页面显示：
   - **Origin Certificate**（`.pem` 格式）
   - **Private Key**（`.key` 格式，**仅显示一次，立即保存**）

---

## 步骤 2：将证书部署到服务器

以 admin 用户 SSH 登录 `8.130.166.83`：

```bash
# 创建证书目录
sudo mkdir -p /etc/ssl/cloudflare
sudo chmod 700 /etc/ssl/cloudflare

# 创建证书文件（粘贴 Cloudflare 页面的 Origin Certificate 内容）
sudo nano /etc/ssl/cloudflare/origin.pem

# 创建私钥文件（粘贴 Cloudflare 页面的 Private Key 内容）
sudo nano /etc/ssl/cloudflare/origin.key

# 设置权限
sudo chmod 644 /etc/ssl/cloudflare/origin.pem
sudo chmod 600 /etc/ssl/cloudflare/origin.key
```

---

## 步骤 3：配置 Nginx 监听 443

编辑（或新建）`/etc/nginx/conf.d/api.shuqixin.conf`：

```nginx
server {
    listen 80;
    server_name api.shuqixin.com.cn;
    # Cloudflare 已处理公网 HTTPS，80 仅供 Cloudflare → 源 的连接
    # 待切换 Full Strict 后，此 server 块可删除或重定向到 443
    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl;
    server_name api.shuqixin.com.cn;

    ssl_certificate     /etc/ssl/cloudflare/origin.pem;
    ssl_certificate_key /etc/ssl/cloudflare/origin.key;

    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto https;
    }
}
```

重新加载 Nginx：

```bash
sudo nginx -t          # 先测试配置语法
sudo systemctl reload nginx
```

---

## 步骤 4：阿里云防火墙放行 TCP 443

1. 登录阿里云轻量应用服务器控制台 → 防火墙规则
2. 添加入方向规则：
   - **协议**：TCP
   - **端口范围**：443
   - **授权对象**：`0.0.0.0/0`（Cloudflare 的入口 IP 会通过此端口连接源服务器）
3. 保存

---

## 步骤 5：切换 Cloudflare SSL/TLS 模式为 Full (Strict)

1. Cloudflare Dashboard → `shuqixin.com.cn` → **SSL/TLS** → **Overview**
2. 将模式从 **Flexible** 切换为 **Full (Strict)**
3. 等待约 30 秒生效

**验证**：

```bash
# 从公网测试（Cloudflare 代理）
curl -I https://api.shuqixin.com.cn/api/health
# 期望：HTTP/2 200

# 直连服务器 443（验证 Nginx + 证书）
curl -k https://8.130.166.83:443/api/health
# -k 因为 Origin Certificate 不被系统 CA 信任，仅 Cloudflare 信任
# 期望：{"status":"ok","timestamp":"..."}
```

---

## 步骤 6（切换验证通过后）：关闭 8080 外网入方向规则

> **此步骤对应原安全债务第 4 项**：8080 端口当前对公网开放，可绕过 Cloudflare 直接访问 Spring Boot，导致：DDoS 无防护、IP 泄露、Cloudflare WAF 规则无效。

验证 443 正常后，在阿里云控制台防火墙：

1. 找到入方向规则中的 **TCP 8080**
2. **删除**该规则

删除后验证：

```bash
# 应该超时或拒绝连接
curl --connect-timeout 5 http://8.130.166.83:8080/api/health
# 期望：连接超时（curl: (28) Connection timed out）
```

---

## 回滚方案

如果切换 Full (Strict) 后出现 502/503：

1. 立即在 Cloudflare 将模式回退为 **Flexible**（约 30 秒生效）
2. 检查 Nginx 443 配置和证书文件是否正确
3. 检查阿里云防火墙是否已放行 443
4. `sudo journalctl -u nginx --since "5 min ago"` 查看错误日志

---

## 完成后的安全状态

| 检查项 | 状态 |
|--------|------|
| 公网 → Cloudflare | HTTPS (Cloudflare 管理证书) ✅ |
| Cloudflare → 源服务器 | HTTPS Full Strict ✅ |
| 源服务器证书 | Cloudflare Origin Certificate (15年) ✅ |
| 8080 端口公网暴露 | 已关闭 ✅ |
| 443 端口 | 仅 Cloudflare IP 实际使用（防火墙未限制，但 Origin Cert 阻止其他客户端） ✅ |
