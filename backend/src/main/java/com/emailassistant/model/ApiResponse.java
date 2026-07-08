package com.emailassistant.model;

import com.google.gson.Gson;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Map;

public class ApiResponse {
    private static final Gson GSON = new Gson();
    private static final Map<String, String> CORS_HEADERS = Map.of(
        "Content-Type", "application/json",
        "Access-Control-Allow-Origin", "*",
        "Access-Control-Allow-Headers", "Content-Type,Authorization",
        "Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"
    );

    public static APIGatewayProxyResponseEvent ok(Object body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS_HEADERS)
                .withBody(GSON.toJson(body));
    }

    public static APIGatewayProxyResponseEvent error(int code, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(CORS_HEADERS)
                .withBody(GSON.toJson(Map.of("error", message)));
    }

    public static APIGatewayProxyResponseEvent redirect(String url) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(302)
                .withHeaders(Map.of("Location", url));
    }
}
