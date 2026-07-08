package com.emailassistant.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.emailassistant.model.ApiResponse;
import com.emailassistant.model.Email;
import com.emailassistant.service.EmailProcessingService;
import com.emailassistant.util.JwtUtil;

import java.util.List;
import java.util.Map;

public class EmailSyncHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final EmailProcessingService service = new EmailProcessingService();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String userId = extractUserId(event);
            if (userId == null) return ApiResponse.error(401, "Unauthorized");

            List<Email> emails = service.syncEmails(userId);

            // Auto-process all synced emails
            for (Email email : emails) {
                try {
                    service.processEmail(userId, email.getEmailId());
                } catch (Exception e) {
                    context.getLogger().log("Failed to process email " + email.getEmailId() + ": " + e.getMessage());
                }
            }

            return ApiResponse.ok(Map.of("synced", emails.size()));
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
