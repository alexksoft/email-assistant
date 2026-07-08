package com.emailassistant.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.emailassistant.model.ApiResponse;
import com.emailassistant.model.Email;
import com.emailassistant.service.EmailProcessingService;
import com.emailassistant.util.JwtUtil;

import java.util.Map;

public class EmailProcessHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final EmailProcessingService service = new EmailProcessingService();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String userId = extractUserId(event);
            if (userId == null) return ApiResponse.error(401, "Unauthorized");

            Map<String, String> pathParams = event.getPathParameters();
            String emailId = pathParams != null ? pathParams.get("id") : null;
            if (emailId == null) return ApiResponse.error(400, "Email ID required");

            Email processed = service.processEmail(userId, emailId);
            return ApiResponse.ok(processed);
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    private String extractUserId(APIGatewayProxyRequestEvent event) {
        Map<String, String> headers = event.getHeaders();
        if (headers == null) return null;
        String auth = headers.get("Authorization");
        if (auth == null) auth = headers.get("authorization");
        return JwtUtil.extractUserIdFromHeader(auth);
    }
}
