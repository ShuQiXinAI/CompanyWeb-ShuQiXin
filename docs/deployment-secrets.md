# 生产环境密钥配置说明

本项目使用环境变量或服务器本地配置文件来注入敏感凭据，**不得将明文密码提交至 git**。

## 需要配置的密钥

| 环境变量 / 属性键 | 说明 |
|---|---|
| `DB_PASSWORD` | MySQL 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥（建议 ≥64 字节随机字符串） |

---

## 方式一：服务器本地 properties 文件（推荐）

1. 在服务器创建文件 `/opt/shuqixin/application-local.properties`（仅 admin 用户可读）：

```properties
spring.datasource.password=<你的数据库密码>
jwt.secret=<你的JWT密钥>
```

2. 设置权限：

```bash
chmod 600 /opt/shuqixin/application-local.properties
```

3. 启动 Spring Boot 时追加 `--spring.config.additional-location` 参数：

```bash
java -jar backend.jar \
  --spring.config.additional-location=file:/opt/shuqixin/
```

> Spring Boot 会合并 classpath 内的 `application.properties` 与本地文件，本地文件优先级更高。

---

## 方式二：操作系统环境变量

在服务器的 systemd 单元文件（或 `/etc/environment`）中添加：

```ini
[Service]
Environment="DB_PASSWORD=<你的数据库密码>"
Environment="JWT_SECRET=<你的JWT密钥>"
```

重载并重启服务：

```bash
sudo systemctl daemon-reload
sudo systemctl restart shuqixin-backend
```

---

## 生成强随机 JWT Secret

```bash
openssl rand -base64 64
```

将输出的字符串填入 `jwt.secret`。

---

## 紧急操作：历史密码已泄露

原密码 `Snowshine@2023!` 已出现在 git commit `5b56946` 的历史记录中。
**合并 fix(security): move DB password and JWT secret out of git 这个 PR 时，维护者必须同步执行以下步骤：**

### (a) 轮换 MySQL root 密码

```sql
ALTER USER 'root'@'localhost' IDENTIFIED BY '<新密码>';
FLUSH PRIVILEGES;
```

### (b) 生成新 JWT Secret 并写入本地配置

```bash
openssl rand -base64 64
# 将输出写入 /opt/shuqixin/application-local.properties 的 jwt.secret
```

### (c) 重启 Spring Boot

```bash
sudo systemctl restart shuqixin-backend
# 或
sudo java -jar backend.jar --spring.config.additional-location=file:/opt/shuqixin/
```

### (d) 验证

访问 `https://api.shuqixin.com.cn/api/health`（PR 5 合并后），确认服务正常返回 `{"status":"ok"}`。

---

> 注意：git 历史无法通过 `.gitignore` 抹除，密码必须通过**轮换**来失效。
> 如需彻底清除历史（可选），可使用 `git filter-repo` 或联系 GitHub Support，但这会重写所有 commit hash，需所有协作者重新 clone。
