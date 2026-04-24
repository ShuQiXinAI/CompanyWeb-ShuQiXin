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
