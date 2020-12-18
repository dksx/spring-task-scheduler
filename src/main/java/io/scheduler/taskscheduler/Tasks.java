package io.scheduler.taskscheduler;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableAsync
public class Tasks {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final String JWKS_URI = "https://dev-136034.okta.com/oauth2/default/v1/keys";
    //private static final String JWKS_URI = "https://vidispinecs.b2clogin.com/vidispinecs.onmicrosoft.com/b2c_1_register_login/discovery/v2.0/keys";
    private static final HttpClient httpClient = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    @Async
    @Scheduled(fixedRate = 10000, initialDelay = 4000)
    public void updateConfiguration() throws InvalidKeySpecException, NoSuchAlgorithmException {
        System.out.println("Executing task - " + dateFormat.format(new Date()));

        String body = fetchKeySet(JWKS_URI, 10000);
        JSONArray keys = new JSONObject(body).getJSONArray("keys");
        List<String> publicKeys = new ArrayList<>(keys.length());
        for (int i = 0; i < keys.length(); i++) {
            String exponent = keys.getJSONObject(i).getString("e");
            String modulus = keys.getJSONObject(i).getString("n");
            publicKeys.add(generateKey(exponent, modulus));
        }
        System.out.println(publicKeys);
    }

    private String generateKey(String exp, String mod) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] moduloBytes = Base64.getUrlDecoder().decode(mod);
        BigInteger modulus = new BigInteger(1, moduloBytes);
        byte[] exponentByte = Base64.getDecoder().decode(exp);
        BigInteger exponent = new BigInteger(1, exponentByte);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey pk = factory.generatePublic(spec);
        return Base64.getEncoder().encodeToString(pk.getEncoded());
    }

    private String fetchKeySet(String uri, int timeout) {
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .build();

        CompletableFuture<String> response = httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
        //response.thenAccept(System.out::println).join();
        return response.join();
    }
}
