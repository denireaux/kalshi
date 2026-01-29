package com.kalshi.api.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.kalshi.api.config.KalshiProperties;

@Component
public class KalshiKeyProvider {

    private final PrivateKey privateKey;

    public KalshiKeyProvider(KalshiProperties props) throws Exception {
        String pem = Files.readString(Path.of(props.privateKeyPath()), StandardCharsets.UTF_8).trim();

        if (pem.contains("BEGIN OPENSSH PRIVATE KEY")) {
            throw new IllegalStateException(
                "Your key is in OPENSSH format. Convert it to PKCS#8 PEM (BEGIN PRIVATE KEY) before using it."
            );
        }

        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            // PKCS#1 -> wrap into PKCS#8
            byte[] pkcs1 = decodePem(pem, "RSA PRIVATE KEY");
            byte[] pkcs8 = wrapPkcs1ToPkcs8(pkcs1);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            return;
        }

        if (pem.contains("BEGIN PRIVATE KEY")) {
            // PKCS#8
            byte[] pkcs8 = decodePem(pem, "PRIVATE KEY");

            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            return;
        }

        throw new IllegalStateException(
            "Unsupported private key format. Expected PEM with 'BEGIN PRIVATE KEY' (PKCS#8) or 'BEGIN RSA PRIVATE KEY' (PKCS#1)."
        );
    }

    public PrivateKey get() {
        return privateKey;
    }

    private static byte[] decodePem(String pem, String type) {
        String normalized = pem
            .replace("-----BEGIN " + type + "-----", "")
            .replace("-----END " + type + "-----", "")
            .replaceAll("\\s", ""); // remove newlines/spaces

        return Base64.getDecoder().decode(normalized);
    }

    /**
     * Minimal PKCS#1 -> PKCS#8 wrapper for RSA private keys.
     * Produces: PrivateKeyInfo { algorithm=rsaEncryption, privateKey=RSAPrivateKey }
     */
    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
        // DER encoding:
        // SEQUENCE(
        //   INTEGER 0
        //   SEQUENCE( OID rsaEncryption, NULL )
        //   OCTET STRING( pkcs1 )
        // )
        // This is a small DER builder, no external deps.

        byte[] algId = derSequence(
            derOid("1.2.840.113549.1.1.1"), // rsaEncryption
            derNull()
        );

        byte[] version = derInteger(0);
        byte[] privateKeyOctet = derOctetString(pkcs1);

        return derSequence(version, algId, privateKeyOctet);
    }

    // ---- tiny DER helpers ----

    private static byte[] derSequence(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        return concat(new byte[]{0x30}, derLength(len), concat(parts));
    }

    private static byte[] derInteger(int val) {
        // only supports small positive ints
        return new byte[]{0x02, 0x01, (byte) val};
    }

    private static byte[] derNull() {
        return new byte[]{0x05, 0x00};
    }

    private static byte[] derOctetString(byte[] data) {
        return concat(new byte[]{0x04}, derLength(data.length), data);
    }

    private static byte[] derOid(String oid) {
        String[] arcs = oid.split("\\.");
        int first = Integer.parseInt(arcs[0]);
        int second = Integer.parseInt(arcs[1]);

        byte[] body = new byte[0];
        body = concat(body, new byte[]{(byte) (first * 40 + second)});

        for (int i = 2; i < arcs.length; i++) {
            long v = Long.parseLong(arcs[i]);
            body = concat(body, encodeBase128(v));
        }

        return concat(new byte[]{0x06}, derLength(body.length), body);
    }

    private static byte[] encodeBase128(long value) {
        // base-128 with continuation bit
        byte[] tmp = new byte[10];
        int pos = tmp.length;
        tmp[--pos] = (byte) (value & 0x7F);
        value >>= 7;
        while (value > 0) {
            tmp[--pos] = (byte) ((value & 0x7F) | 0x80);
            value >>= 7;
        }
        byte[] out = new byte[tmp.length - pos];
        System.arraycopy(tmp, pos, out, 0, out.length);
        return out;
    }

    private static byte[] derLength(int len) {
        if (len < 128) return new byte[]{(byte) len};
        if (len < 256) return new byte[]{(byte) 0x81, (byte) len};
        return new byte[]{(byte) 0x82, (byte) (len >> 8), (byte) len};
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] out = new byte[total];
        int i = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, i, a.length);
            i += a.length;
        }
        return out;
    }
}
