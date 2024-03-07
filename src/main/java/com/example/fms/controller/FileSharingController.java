package com.example.fms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileSharingController {
    
    @Value("${NAME:World}")
    String name;
    
    @PutMapping("/")
    String hello() {
      return "Hello " + name + "!";
    }

}
