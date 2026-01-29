package com.kalshi.api.security;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.kalshi.api.config.KalshiProperties;

@Component
public class KalshiSigner {

    private final KalshiProperties props;
    private final PrivateKey privateKey;

    public KalshiSigner(KalshiProperties props, KalshiKeyProvider keys) {
        this.props = props;
        this.privateKey = keys.get();
    }

    public Map<String, String> sign(String method, String path, String body) throws Exception {
        long ts = Instant.now().getEpochSecond();

        // Kalshi expects: timestamp + method + path + body
        String payload = ts + method.toUpperCase() + path + (body == null ? "" : body);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));

        String sigB64 = Base64.getEncoder().encodeToString(signature.sign());

        return Map.of(
                "KALSHI-ACCESS-KEY", props.apiKeyId(),
                "KALSHI-ACCESS-TIMESTAMP", String.valueOf(ts),
                "KALSHI-ACCESS-SIGNATURE", sigB64
        );
    }
}
