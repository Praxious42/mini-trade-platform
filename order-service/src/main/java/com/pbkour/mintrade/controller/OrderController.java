package com.pbkour.mintrade.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("api/v1/orders")
public class OrderController {
    @PostMapping
    public void createOrder() {
        log.info("Hello world");
        //TODO implement
    }

    @PostMapping("/{id}/cancel")
    public void cancelOrder() {
        //TODO implement
    }

    @RequestMapping("/{id}")
    public void getOrder() {
        //TODO implement
    }

    @RequestMapping
    public void listOrdersByAccount() {
        //TODO implement
    }
}
