package com.chubb.claims.web;

import com.chubb.claims.dto.Dtos.UserDto;
import com.chubb.claims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Backs the "pick a user" mock login on the frontend — see README auth shortcut. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserDto> list() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }
}
