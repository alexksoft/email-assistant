package com.emailassistant.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.emailassistant.model.User;
import com.emailassistant.util.DynamoDbUtil;
import com.emailassistant.util.JwtUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class AuthService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final String GMAIL_CLIENT_ID = System.getenv("GMAIL_CLIENT_ID");
    private static final String GMAIL_CLIENT_SECRET = System.getenv("GMAIL_CLIENT_SECRET");
    private static final String OUTLOOK_CLIENT_ID = System.getenv("OUTLOOK_CLIENT_ID");
    private static final String OUTLOOK_CLIENT_SECRET = System.getenv("OUTLOOK_CLIENT_SECRET");
    private static final String OAUTH_REDIRECT_URI = System.getenv("OAUTH_REDIRECT_URI");

    public Map<String, String> register(String email, String password, String name) {
        DynamoDbTable<User> table = DynamoDbUtil.usersTable();

        // Check if user exists
        var existingUsers = table.index("email-index")
                .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build()));
        if (existingUsers.stream().flatMap(p -> p.items().stream()).findFirst().isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
        user.setName(name);
        user.setCreatedAt(System.currentTimeMillis());
        table.putItem(user);

        String token = JwtUtil.generateToken(user.getUserId(), email);
        return Map.of("token", token, "userId", user.getUserId());
    }

    public Map<String, String> login(String email, String password) {
        DynamoDbTable<User> table = DynamoDbUtil.usersTable();

        User user = table.index("email-index")
                .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build()))
                .stream().flatMap(p -> p.items().stream()).findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = JwtUtil.generateToken(user.getUserId(), email);
        return Map.of("token", token, "userId", user.getUserId());
    }

    public String getGmailOAuthUrl(String userId) {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + encode(GMAIL_CLIENT_ID) +
                "&redirect_uri=" + encode(OAUTH_REDIRECT_URI) +
                "&response_type=code" +
                "&scope=" + encode("https://www.googleapis.com/auth/gmail.readonly") +
                "&access_type=offline" +
                "&state=gmail:" + userId;
    }

    public String getOutlookOAuthUrl(String userId) {
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?" +
                "client_id=" + encode(OUTLOOK_CLIENT_ID) +
                "&redirect_uri=" + encode(OAUTH_REDIRECT_URI) +
                "&response_type=code" +
                "&scope=" + encode("https://graph.microsoft.com/Mail.Read offline_access") +
                "&state=outlook:" + userId;
    }

    public void handleOAuthCallback(String code, String state) throws Exception {
        String[] parts = state.split(":");
        String provider = parts[0];
        String userId = parts[1];

        JsonObject tokens;
        if ("gmail".equals(provider)) {
            tokens = exchangeGmailCode(code);
            saveGmailTokens(userId, tokens);
        } else {
            tokens = exchangeOutlookCode(code);
            saveOutlookTokens(userId, tokens);
        }
    }

    private JsonObject exchangeGmailCode(String code) throws Exception {
        String body = "code=" + encode(code) +
                "&client_id=" + encode(GMAIL_CLIENT_ID) +
                "&client_secret=" + encode(GMAIL_CLIENT_SECRET) +
                "&redirect_uri=" + encode(OAUTH_REDIRECT_URI) +
                "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private JsonObject exchangeOutlookCode(String code) throws Exception {
        String body = "code=" + encode(code) +
                "&client_id=" + encode(OUTLOOK_CLIENT_ID) +
                "&client_secret=" + encode(OUTLOOK_CLIENT_SECRET) +
                "&redirect_uri=" + encode(OAUTH_REDIRECT_URI) +
                "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/common/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private void saveGmailTokens(String userId, JsonObject tokens) {
        DynamoDbTable<User> table = DynamoDbUtil.usersTable();
        User user = table.getItem(Key.builder().partitionValue(userId).build());
        user.setGmailAccessToken(tokens.get("access_token").getAsString());
        if (tokens.has("refresh_token")) {
            user.setGmailRefreshToken(tokens.get("refresh_token").getAsString());
        }
        user.setConnectedProvider("gmail");
        table.updateItem(user);
    }

    private void saveOutlookTokens(String userId, JsonObject tokens) {
        DynamoDbTable<User> table = DynamoDbUtil.usersTable();
        User user = table.getItem(Key.builder().partitionValue(userId).build());
        user.setOutlookAccessToken(tokens.get("access_token").getAsString());
        if (tokens.has("refresh_token")) {
            user.setOutlookRefreshToken(tokens.get("refresh_token").getAsString());
        }
        user.setConnectedProvider("outlook");
        table.updateItem(user);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
