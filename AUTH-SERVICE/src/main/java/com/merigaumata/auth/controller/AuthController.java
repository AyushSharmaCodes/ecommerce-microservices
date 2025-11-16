package com.merigaumata.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<String> loginUser() {
        return new ResponseEntity<>("User logged in successfully ", HttpStatus.OK);
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateUser() {
        return new ResponseEntity<>("User validated successfully ", HttpStatus.OK);
    }
}
