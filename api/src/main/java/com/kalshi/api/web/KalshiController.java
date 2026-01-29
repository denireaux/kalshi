package com.kalshi.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kalshi.api.client.KalshiClient;


@RestController
@RequestMapping("/kalshi")
public class KalshiController {
    private final KalshiClient kalshiClient; 

    public KalshiController(KalshiClient client) {
        this.kalshiClient = client;
    }

    @GetMapping("/markets")
    public String markets() throws Exception {
        return kalshiClient.getMarkets();
    }
}
