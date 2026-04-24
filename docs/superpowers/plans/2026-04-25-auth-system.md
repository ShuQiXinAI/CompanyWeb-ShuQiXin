# Auth System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user registration and login to the ShuQiXin website with a Spring Boot backend on Alibaba Cloud and matching dark-theme frontend pages.

**Architecture:** Static HTML frontend (React 18 + Babel Standalone) served by Cloudflare Pages communicates via REST API with a Spring Boot 3 backend on Alibaba Cloud (8.130.166.83). JWT tokens stored in localStorage authenticate requests. MySQL 8 stores user data.

**Tech Stack:** Java 17, Spring Boot 3, MySQL 8, JWT (jjwt), BCrypt, React 18 (CDN)

---

## File Structure

### Frontend (Cloudflare Pages — project root)

| File | Responsibility |
|------|---------------|
| `login.html` | Login page — standalone HTML with React, same tech as index.html |
| `register.html` | Register page — standalone HTML with React, same tech as index.html |
| `index.html` | Modify Nav component to support logged-in state |

### Backend (Spring Boot — `backend/` directory)

| File | Responsibility |
|------|---------------|
| `backend/pom.xml` | Maven project with Spring Boot, MySQL, JWT dependencies |
| `backend/src/main/resources/application.properties` | DB connection, JWT secret, CORS config |
| `backend/src/main/java/com/shuqixin/auth/AuthApplication.java` | Spring Boot main class |
| `backend/src/main/java/com/shuqixin/auth/entity/User.java` | JPA entity mapping `users` table |
| `backend/src/main/java/com/shuqixin/auth/dto/AuthRequest.java` | Request DTO for login/register |
| `backend/src/main/java/com/shuqixin/auth/dto/ApiResponse.java` | Unified response wrapper |
| `backend/src/main/java/com/shuqixin/auth/repository/UserRepository.java` | Spring Data JPA repository |
| `backend/src/main/java/com/shuqixin/auth/service/AuthService.java` | Business logic: register, login, verify |
| `backend/src/main/java/com/shuqixin/auth/util/JwtUtil.java` | JWT token generation and validation |
| `backend/src/main/java/com/shuqixin/auth/controller/AuthController.java` | REST endpoints: /api/auth/* |
| `backend/src/main/java/com/shuqixin/auth/config/CorsConfig.java` | CORS configuration |
| `backend/src/main/java/com/shuqixin/auth/config/SecurityConfig.java` | Spring Security: permit auth endpoints, disable CSRF |

### Database

| Script | Responsibility |
|--------|---------------|
| `backend/src/main/resources/schema.sql` | CREATE DATABASE + CREATE TABLE users |

---

## Task 1: Database Setup

**Files:**
- Create: `backend/src/main/resources/schema.sql`

- [ ] **Step 1: Write the SQL schema file**

```sql
CREATE DATABASE IF NOT EXISTS shuqixin DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE shuqixin;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone VARCHAR(11) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt encrypted',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Execute schema on the Alibaba Cloud MySQL instance**

Run via SSH on 8.130.166.83:

```bash
mysql -u root -psnowshine < /path/to/schema.sql
```

Expected: database `shuqixin` created, table `users` created with 0 rows.

- [ ] **Step 3: Verify**

```bash
mysql -u root -psnowshine -e "USE shuqixin; DESCRIBE users;"
```

Expected: table with columns `id`, `phone`, `password`, `created_at`, `updated_at`.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/schema.sql
git commit -m "feat(db): add users table schema for auth system"
```

---

## Task 2: Spring Boot Project Scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.properties`
- Create: `backend/src/main/java/com/shuqixin/auth/AuthApplication.java`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.5</version>
    </parent>
    <groupId>com.shuqixin</groupId>
    <artifactId>auth</artifactId>
    <version>1.0.0</version>
    <name>shuqixin-auth</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.properties**

```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/shuqixin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=snowshine
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

jwt.secret=ShuQiXin2026SecretKeyForJwtTokenSigningMustBe256BitsLong!!
jwt.expiration-ms=604800000

spring.jackson.date-format=yyyy-MM-dd'T'HH:mm:ss
spring.jackson.time-zone=Asia/Shanghai
```

- [ ] **Step 3: Create AuthApplication.java**

```java
package com.shuqixin.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/
git commit -m "feat(backend): scaffold Spring Boot project with dependencies"
```

---

## Task 3: Entity, DTOs, and Repository

**Files:**
- Create: `backend/src/main/java/com/shuqixin/auth/entity/User.java`
- Create: `backend/src/main/java/com/shuqixin/auth/dto/AuthRequest.java`
- Create: `backend/src/main/java/com/shuqixin/auth/dto/ApiResponse.java`
- Create: `backend/src/main/java/com/shuqixin/auth/repository/UserRepository.java`

- [ ] **Step 1: Create User entity**

```java
package com.shuqixin.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 2: Create AuthRequest DTO**

```java
package com.shuqixin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 3: Create ApiResponse DTO**

```java
package com.shuqixin.auth.dto;

import java.util.Map;

public class ApiResponse {

    private int code;
    private String message;
    private Object data;

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(200, message, data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
```

- [ ] **Step 4: Create UserRepository**

```java
package com.shuqixin.auth.repository;

import com.shuqixin.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/shuqixin/auth/entity/ backend/src/main/java/com/shuqixin/auth/dto/ backend/src/main/java/com/shuqixin/auth/repository/
git commit -m "feat(backend): add User entity, DTOs, and repository"
```

---

## Task 4: JWT Utility

**Files:**
- Create: `backend/src/main/java/com/shuqixin/auth/util/JwtUtil.java`

- [ ] **Step 1: Create JwtUtil**

```java
package com.shuqixin.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String phone) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/shuqixin/auth/util/
git commit -m "feat(backend): add JWT utility for token generation and validation"
```

---

## Task 5: Auth Service

**Files:**
- Create: `backend/src/main/java/com/shuqixin/auth/service/AuthService.java`

- [ ] **Step 1: Create AuthService**

```java
package com.shuqixin.auth.service;

import com.shuqixin.auth.dto.ApiResponse;
import com.shuqixin.auth.dto.AuthRequest;
import com.shuqixin.auth.entity.User;
import com.shuqixin.auth.repository.UserRepository;
import com.shuqixin.auth.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public ApiResponse register(AuthRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            return ApiResponse.error(400, "该手机号已注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getPhone());
        return ApiResponse.ok("注册成功", Map.of(
                "token", token,
                "phone", user.getPhone()
        ));
    }

    public ApiResponse login(AuthRequest request) {
        Optional<User> optUser = userRepository.findByPhone(request.getPhone());
        if (optUser.isEmpty() || !passwordEncoder.matches(request.getPassword(), optUser.get().getPassword())) {
            return ApiResponse.error(401, "手机号或密码错误");
        }

        User user = optUser.get();
        String token = jwtUtil.generateToken(user.getId(), user.getPhone());
        return ApiResponse.ok("登录成功", Map.of(
                "token", token,
                "phone", user.getPhone()
        ));
    }

    public ApiResponse me(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ApiResponse.error(401, "token 无效或已过期");
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            return ApiResponse.error(401, "token 无效或已过期");
        }

        var claims = jwtUtil.parseToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        String phone = claims.get("phone", String.class);

        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return ApiResponse.error(401, "token 无效或已过期");
        }

        User user = optUser.get();
        return ApiResponse.ok(null, Map.of(
                "id", user.getId(),
                "phone", user.getPhone(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/shuqixin/auth/service/
git commit -m "feat(backend): add AuthService with register, login, me logic"
```

---

## Task 6: Controller and Config

**Files:**
- Create: `backend/src/main/java/com/shuqixin/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/shuqixin/auth/config/CorsConfig.java`
- Create: `backend/src/main/java/com/shuqixin/auth/config/SecurityConfig.java`

- [ ] **Step 1: Create AuthController**

```java
package com.shuqixin.auth.controller;

import com.shuqixin.auth.dto.ApiResponse;
import com.shuqixin.auth.dto.AuthRequest;
import com.shuqixin.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody AuthRequest request) {
        ApiResponse response = authService.register(request);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody AuthRequest request) {
        ApiResponse response = authService.login(request);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ApiResponse response = authService.me(authHeader);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }
}
```

- [ ] **Step 2: Create CorsConfig**

```java
package com.shuqixin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 3: Create SecurityConfig**

```java
package com.shuqixin.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/shuqixin/auth/controller/ backend/src/main/java/com/shuqixin/auth/config/
git commit -m "feat(backend): add AuthController, CORS and Security config"
```

---

## Task 7: Build and Deploy Backend to Alibaba Cloud

- [ ] **Step 1: Build the JAR locally**

```bash
cd backend && ./mvnw clean package -DskipTests
```

Expected: `target/auth-1.0.0.jar` created successfully.

- [ ] **Step 2: Upload JAR to Alibaba Cloud server**

```bash
scp target/auth-1.0.0.jar root@8.130.166.83:/opt/shuqixin/
```

- [ ] **Step 3: SSH into server and run the schema**

```bash
ssh root@8.130.166.83
mysql -u root -psnowshine < /opt/shuqixin/schema.sql
```

Also upload the schema file:
```bash
scp backend/src/main/resources/schema.sql root@8.130.166.83:/opt/shuqixin/
```

- [ ] **Step 4: Start Spring Boot on the server**

```bash
ssh root@8.130.166.83
cd /opt/shuqixin
nohup java -jar auth-1.0.0.jar > app.log 2>&1 &
```

- [ ] **Step 5: Verify API is running**

```bash
curl http://8.130.166.83:8080/api/auth/me
```

Expected: `{"code":401,"message":"token 无效或已过期","data":null}`

- [ ] **Step 6: Test register endpoint**

```bash
curl -X POST http://8.130.166.83:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"Test1234"}'
```

Expected: `{"code":200,"message":"注册成功","data":{"token":"eyJ...","phone":"13800138000"}}`

- [ ] **Step 7: Test login endpoint**

```bash
curl -X POST http://8.130.166.83:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"Test1234"}'
```

Expected: `{"code":200,"message":"登录成功","data":{"token":"eyJ...","phone":"13800138000"}}`

- [ ] **Step 8: Commit (no code changes, just note deployment)**

```bash
git commit --allow-empty -m "ops: deploy backend v1.0.0 to Alibaba Cloud 8.130.166.83"
```

---

## Task 8: Frontend — Login Page

**Files:**
- Create: `login.html`

- [ ] **Step 1: Create login.html**

The file must include:
- Identical `<head>` section as `index.html` (same CSS variables, fonts, noise overlay, scrollbar styles)
- Same React 18 + ReactDOM 18 + Babel Standalone CDN scripts
- Reuse `LogoA` component from index.html (copy the SVG component)
- Full-screen layout: `background: var(--bg)` with grid texture overlay and radial gradient glow
- Centered glassmorphism card (max-width 420px):
  - LogoA icon centered at top
  - Title "欢迎回来" (serif, 24px, #F8FAFC)
  - Subtitle "登录你的数启欣账号" (13px, --text-3)
  - Phone field: label "手机号 PHONE" (mono, 11px, uppercase), underline input, placeholder "请输入手机号"
  - Password field: label "密码 PASSWORD" (mono, 11px, uppercase), underline input type=password, placeholder "请输入密码"
  - Submit button: full-width, accent bg, "登录 →", border-radius 12px
  - Bottom link: "还没有账号？" + accent-colored "立即注册 →" linking to `/register.html`
- Form validation matching spec: phone regex `/^1[3-9]\d{9}$/`, password required
- On success: save token to `localStorage.setItem('token', data.token)`, save phone to `localStorage.setItem('phone', data.phone)`, redirect to `/index.html`
- On error: display server error message below the button in red
- API endpoint: `http://8.130.166.83:8080/api/auth/login`
- "返回首页" link at top-left corner linking to `/index.html`

- [ ] **Step 2: Open in browser and test**

Verify: page renders with dark theme, form validates, login with the test account `13800138000 / Test1234` succeeds and redirects to index.html.

- [ ] **Step 3: Commit**

```bash
git add login.html
git commit -m "feat(frontend): add login page with dark theme and form validation"
```

---

## Task 9: Frontend — Register Page

**Files:**
- Create: `register.html`

- [ ] **Step 1: Create register.html**

Identical structure to `login.html` with these differences:
- Title: "创建账号" instead of "欢迎回来"
- Subtitle: "加入数启欣，开启智能之旅" instead of "登录你的数启欣账号"
- Three fields instead of two: phone, password, confirm password
- Password validation: at least 8 chars, must contain both letters and numbers (regex: `/^(?=.*[a-zA-Z])(?=.*\d).{8,}$/`)
- Confirm password: must match password field
- Submit button text: "注册 →"
- Bottom link: "已有账号？" + "立即登录 →" linking to `/login.html`
- API endpoint: `http://8.130.166.83:8080/api/auth/register`
- On success: save token and phone to localStorage, redirect to `/index.html`

- [ ] **Step 2: Open in browser and test**

Verify: register with a new phone number succeeds, token saved, redirected to index.html.

- [ ] **Step 3: Commit**

```bash
git add register.html
git commit -m "feat(frontend): add register page with password confirmation"
```

---

## Task 10: Frontend — Modify Nav for Logged-In State

**Files:**
- Modify: `index.html` (Nav component, lines 159-224; App component, lines 1075-1094)

- [ ] **Step 1: Update Nav component to accept `user` and `onLogout` props**

Change the Nav signature from:
```jsx
const Nav = ({ accent }) => {
```
to:
```jsx
const Nav = ({ accent, user, onLogout }) => {
```

Replace the "开始合作" link block (the `<a href="#contact" ...>开始合作</a>` at lines 211-222) with:

```jsx
{user ? (
  <div style={{display:'flex', alignItems:'center', gap:12}}>
    <div style={{fontSize:13, color:'var(--text-2)', letterSpacing:'0.05em'}}>
      用户 *{user.phone.slice(-4)}
    </div>
    <button onClick={onLogout} style={{
      display:'inline-flex', alignItems:'center', gap:6,
      padding:'8px 16px', borderRadius:999,
      border:'1px solid rgba(150,200,235,0.2)', color:'var(--text-3)', fontSize:12,
      transition:'all 0.2s',
    }}
    onMouseEnter={e=>{e.currentTarget.style.borderColor='#F87171';e.currentTarget.style.color='#F87171'}}
    onMouseLeave={e=>{e.currentTarget.style.borderColor='rgba(150,200,235,0.2)';e.currentTarget.style.color='var(--text-3)'}}
    >退出</button>
  </div>
) : (
  <a href="/login.html" style={{
    display:'inline-flex', alignItems:'center', gap:8,
    padding:'10px 20px', borderRadius:999,
    border:`1px solid ${accent}`, color:accent, fontSize:13, letterSpacing:'0.08em', fontWeight:500,
    transition:'all 0.2s',
  }}
  onMouseEnter={e=>{e.currentTarget.style.background=accent;e.currentTarget.style.color='#0E1A2E'}}
  onMouseLeave={e=>{e.currentTarget.style.background='transparent';e.currentTarget.style.color=accent}}
  >
    开始合作
    <span style={{fontSize:16,lineHeight:1}}>→</span>
  </a>
)}
```

- [ ] **Step 2: Update App component to manage auth state**

Replace the App component (lines 1075-1094) with:

```jsx
const App = () => {
  const accent = '#4DD4AC';
  const [user, setUser] = React.useState(null);

  React.useEffect(() => {
    const token = localStorage.getItem('token');
    const phone = localStorage.getItem('phone');
    if (token && phone) {
      fetch('http://8.130.166.83:8080/api/auth/me', {
        headers: { 'Authorization': 'Bearer ' + token }
      })
      .then(r => r.json())
      .then(res => {
        if (res.code === 200) {
          setUser({ phone: res.data.phone });
        } else {
          localStorage.removeItem('token');
          localStorage.removeItem('phone');
        }
      })
      .catch(() => {});
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('phone');
    setUser(null);
  };

  return (
    <div style={{position:'relative', zIndex:2}}>
      <Nav accent={accent} user={user} onLogout={handleLogout}/>
      <Hero accent={accent}/>
      <Services accent={accent}/>
      <Product accent={accent}/>
      <Vision accent={accent}/>
      <Culture accent={accent}/>
      <About accent={accent}/>
      <Contact accent={accent}/>
    </div>
  );
};
```

- [ ] **Step 3: Open index.html in browser and test**

1. Not logged in: Nav shows "开始合作" button linking to `/login.html`
2. Log in via login.html, get redirected back
3. Nav now shows "用户 *8000" + "退出" button
4. Click "退出": Nav reverts to "开始合作"

- [ ] **Step 4: Commit**

```bash
git add index.html
git commit -m "feat(frontend): add auth state to Nav — show user info or login link"
```

---

## Task 11: Push to GitHub and Deploy

- [ ] **Step 1: Add .superpowers to .gitignore**

Append to `.gitignore`:
```
# Superpowers brainstorming
.superpowers/
```

- [ ] **Step 2: Push all changes to GitHub**

```bash
git add .gitignore
git commit -m "chore: ignore .superpowers directory"
git push origin master
```

- [ ] **Step 3: Verify Cloudflare Pages deployment**

Cloudflare Pages auto-deploys from GitHub. Visit the Cloudflare Pages URL and verify:
- `/index.html` loads, Nav shows "开始合作"
- `/login.html` renders login form
- `/register.html` renders register form
- Full login → redirect → Nav update → logout flow works

- [ ] **Step 4: Verify backend is still running on Alibaba Cloud**

```bash
curl http://8.130.166.83:8080/api/auth/me
```

Expected: 401 response confirming the API is live.
