package com.kalshi.api.client;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.kalshi.api.security.KalshiSigner;

@Component
public class KalshiClient {

    private final RestClient rest;
    private final KalshiSigner signer;

    public KalshiClient(RestClient kalshiRestClient, KalshiSigner signer) {
        this.rest = kalshiRestClient;
        this.signer = signer;
    }

    public String getMarkets() throws Exception {
        String path = "/trade-api/v2/markets";
        String body = "";

        Map<String, String> headers = signer.sign("GET", path, body);

        return rest.get()
                .uri(path)
                .headers(h -> headers.forEach(h::add))
                .retrieve()
                .body(String.class);
    }
}
