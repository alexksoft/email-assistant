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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OutlookService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0/me/messages";

    public List<Email> fetchRecentEmails(String userId, String accessToken, int maxResults) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_API + "?$top=" + maxResults + "&$orderby=receivedDateTime desc" +
                        "&$select=id,subject,from,toRecipients,body,bodyPreview,receivedDateTime"))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);

        if (!json.has("value")) return List.of();

        JsonArray messages = json.getAsJsonArray("value");
        List<Email> emails = new ArrayList<>();

        for (JsonElement msg : messages) {
            JsonObject m = msg.getAsJsonObject();
            Email email = new Email();
            email.setUserId(userId);
            email.setEmailId(m.get("id").getAsString());
            email.setProvider("outlook");
            email.setStatus("unprocessed");
            email.setSubject(getStr(m, "subject"));
            email.setSnippet(getStr(m, "bodyPreview"));

            if (m.has("from") && m.getAsJsonObject("from").has("emailAddress")) {
                email.setFrom(m.getAsJsonObject("from").getAsJsonObject("emailAddress").get("address").getAsString());
            }
            if (m.has("toRecipients")) {
                JsonArray to = m.getAsJsonArray("toRecipients");
                if (!to.isEmpty()) {
                    email.setTo(to.get(0).getAsJsonObject().getAsJsonObject("emailAddress").get("address").getAsString());
                }
            }
            if (m.has("body")) {
                email.setBody(m.getAsJsonObject("body").get("content").getAsString());
            }
            if (m.has("receivedDateTime")) {
                email.setReceivedAt(Instant.parse(m.get("receivedDateTime").getAsString()).toEpochMilli());
            }
            emails.add(email);
        }
        return emails;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
