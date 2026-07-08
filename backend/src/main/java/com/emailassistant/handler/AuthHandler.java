package com.emailassistant.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.emailassistant.model.ApiResponse;
import com.emailassistant.service.AuthService;
import com.emailassistant.util.JwtUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String path = event.getPath();
        String method = event.getHttpMethod();

        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                return handleRegister(event);
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                return handleLogin(event);
            } else if ("GET".equals(method) && path.contains("/oauth/gmail")) {
                return handleGmailOAuth(event);
            } else if ("GET".equals(method) && path.contains("/oauth/outlook")) {
                return handleOutlookOAuth(event);
            } else if ("GET".equals(method) && path.contains("/oauth/callback")) {
                return handleOAuthCallback(event);
            }
            return ApiResponse.error(404, "Not found");
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleRegister(APIGatewayProxyRequestEvent event) {
        JsonObject body = gson.fromJson(event.getBody(), JsonObject.class);
        Map<String, String> result = authService.register(
                body.get("email").getAsString(),
                body.get("password").getAsString(),
                body.get("name").getAsString()
        );
        return ApiResponse.ok(result);
    }

    private APIGatewayProxyResponseEvent handleLogin(APIGatewayProxyRequestEvent event) {
        JsonObject body = gson.fromJson(event.getBody(), JsonObject.class);
        Map<String, String> result = authService.login(
                body.get("email").getAsString(),
                body.get("password").getAsString()
        );
        return ApiResponse.ok(result);
    }

    private APIGatewayProxyResponseEvent handleGmailOAuth(APIGatewayProxyRequestEvent event) {
        String userId = extractUserId(event);
        if (userId == null) return ApiResponse.error(401, "Unauthorized");
        String url = authService.getGmailOAuthUrl(userId);
        return ApiResponse.redirect(url);
    }

    private APIGatewayProxyResponseEvent handleOutlookOAuth(APIGatewayProxyRequestEvent event) {
        String userId = extractUserId(event);
        if (userId == null) return ApiResponse.error(401, "Unauthorized");
        String url = authService.getOutlookOAuthUrl(userId);
        return ApiResponse.redirect(url);
    }

    private APIGatewayProxyResponseEvent handleOAuthCallback(APIGatewayProxyRequestEvent event) throws Exception {
        Map<String, String> params = event.getQueryStringParameters();
        String code = params.get("code");
        String state = params.get("state");
        authService.handleOAuthCallback(code, state);
        return ApiResponse.ok(Map.of("message", "Email connected successfully"));
    }

    private String extractUserId(APIGatewayProxyRequestEvent event) {
        Map<String, String> headers = event.getHeaders();
        if (headers == null) return null;
        String auth = headers.get("Authorization");
        if (auth == null) auth = headers.get("authorization");
        return JwtUtil.extractUserIdFromHeader(auth);
    }
}
