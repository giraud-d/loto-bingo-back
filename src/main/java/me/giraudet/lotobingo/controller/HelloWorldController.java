package me.giraudet.lotobingo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/hello-world")
    public String getHelloWorld() {
        return "Hello World!";
    }
}
