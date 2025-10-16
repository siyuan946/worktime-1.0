package com.example.worktime.controller;

import com.example.worktime.model.User;
import com.example.worktime.repository.UserRepository;
import com.example.worktime.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    private final UserRepository repository;
    private final OperationLogService logService;

    public AuthController(UserRepository repository, OperationLogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    @PostMapping("/login")
    public void login(@RequestBody User user) {
        if (!repository.findByUsernameAndPassword(user.getUsername(), user.getPassword()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        logService.log(user.getUsername(), "登录", null);
    }
}


