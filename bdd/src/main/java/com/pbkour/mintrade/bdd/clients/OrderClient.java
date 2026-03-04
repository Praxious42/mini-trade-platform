package com.pbkour.mintrade.bdd.clients;


import com.pbkour.mintrade.commons.dto.Order;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

import java.util.UUID;

public interface OrderClient {
    @RequestLine("GET /{id}")
    @Headers("Content-Type: application/json")
    Response getOrder(@Param("id") UUID id);

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    Response createOrder(Order order);
}
