package com.example.demo.controller.Business;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/business")
public class UserBusinessController {
    @GetMapping("/me")
    public String getCurrentUser() {
        return "hello";
    }
}
