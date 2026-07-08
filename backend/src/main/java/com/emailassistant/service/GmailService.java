package com.emailassistant.service;

import com.emailassistant.model.Email;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GmailService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String GMAIL_API = "https://www.googleapis.com/gmail/v1/users/me";

    public List<Email> fetchRecentEmails(String userId, String accessToken, int maxResults) throws Exception {
        // List message IDs
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(GMAIL_API + "/messages?maxResults=" + maxResults + "&labelIds=INBOX"))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> listResp = HTTP.send(listReq, HttpResponse.BodyHandlers.ofString());
        JsonObject listJson = GSON.fromJson(listResp.body(), JsonObject.class);

        if (!listJson.has("messages")) return List.of();

        JsonArray messages = listJson.getAsJsonArray("messages");
        List<Email> emails = new ArrayList<>();

        for (JsonElement msg : messages) {
            String msgId = msg.getAsJsonObject().get("id").getAsString();
            Email email = fetchSingleEmail(userId, accessToken, msgId);
            if (email != null) emails.add(email);
        }
        return emails;
    }

    private Email fetchSingleEmail(String userId, String accessToken, String messageId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GMAIL_API + "/messages/" + messageId + "?format=full"))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject msgJson = GSON.fromJson(resp.body(), JsonObject.class);

        Email email = new Email();
        email.setUserId(userId);
        email.setEmailId(messageId);
        email.setProvider("gmail");
        email.setStatus("unprocessed");

        if (msgJson.has("snippet")) {
            email.setSnippet(msgJson.get("snippet").getAsString());
        }
        if (msgJson.has("internalDate")) {
            email.setReceivedAt(msgJson.get("internalDate").getAsLong());
        }

        // Extract headers
        JsonObject payload = msgJson.getAsJsonObject("payload");
        if (payload != null && payload.has("headers")) {
            for (JsonElement header : payload.getAsJsonArray("headers")) {
                JsonObject h = header.getAsJsonObject();
                String name = h.get("name").getAsString();
                String value = h.get("value").getAsString();
                switch (name) {
                    case "Subject" -> email.setSubject(value);
                    case "From" -> email.setFrom(value);
                    case "To" -> email.setTo(value);
                }
            }
        }

        // Extract body
        email.setBody(extractBody(payload));
        return email;
    }

    private String extractBody(JsonObject payload) {
        if (payload == null) return "";

        // Check direct body
        if (payload.has("body")) {
            JsonObject body = payload.getAsJsonObject("body");
            if (body.has("data")) {
                return decodeBase64(body.get("data").getAsString());
            }
        }

        // Check parts
        if (payload.has("parts")) {
            for (JsonElement part : payload.getAsJsonArray("parts")) {
                JsonObject p = part.getAsJsonObject();
                String mimeType = p.has("mimeType") ? p.get("mimeType").getAsString() : "";
                if ("text/plain".equals(mimeType) && p.has("body")) {
                    JsonObject body = p.getAsJsonObject("body");
                    if (body.has("data")) {
                        return decodeBase64(body.get("data").getAsString());
                    }
                }
            }
        }
        return "";
    }

    private String decodeBase64(String data) {
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded);
    }
}
