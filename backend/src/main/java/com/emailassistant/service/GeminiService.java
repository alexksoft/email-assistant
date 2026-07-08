package com.emailassistant.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class GeminiService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public Map<String, String> categorizeAndDraft(String subject, String from, String body) throws Exception {
        String prompt = """
                Analyze this email and respond in JSON format with exactly these fields:
                - "category": one of [urgent, sales, support, newsletter, personal, other]
                - "priority": number 1-5 (1=highest priority)
                - "draftReply": a professional reply draft (2-4 sentences)
                
                Respond ONLY with valid JSON, no markdown, no extra text.
                
                Email:
                From: %s
                Subject: %s
                Body: %s
                """.formatted(from, subject, truncate(body, 2000));

        String response = callGemini(prompt);
        JsonObject result = GSON.fromJson(response, JsonObject.class);

        return Map.of(
                "category", result.has("category") ? result.get("category").getAsString() : "other",
                "priority", result.has("priority") ? result.get("priority").getAsString() : "3",
                "draftReply", result.has("draftReply") ? result.get("draftReply").getAsString() : ""
        );
    }

    private String callGemini(String prompt) throws Exception {
        JsonObject requestBody = new JsonObject();

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);

        content.addProperty("role", "user");
        content.add("parts", parts);
        contents.add(content);

        requestBody.add("contents", contents);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.3);
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject respJson = GSON.fromJson(response.body(), JsonObject.class);

        return respJson.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }
}
