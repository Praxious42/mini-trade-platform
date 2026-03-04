package com.pbkour.mintrade.bdd.clients;

import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

public class ClientFactory {
    public OrderClient createOrderClient() {
        return Feign.builder()
            .client(new OkHttpClient())
            .encoder(new JacksonEncoder())
            .decoder(new JacksonDecoder())
            .logger(new Slf4jLogger(OrderClient.class))
            .logLevel(Logger.Level.FULL)
            .target(OrderClient.class, "http://127.0.0.1:8081/api/v1/orders");
    }
}
