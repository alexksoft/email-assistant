package com.emailassistant.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.emailassistant.model.ApiResponse;
import com.emailassistant.model.Email;
import com.emailassistant.util.DynamoDbUtil;
import com.emailassistant.util.JwtUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String userId = extractUserId(event);
            if (userId == null) return ApiResponse.error(401, "Unauthorized");

            String path = event.getPath();

            if (path.endsWith("/stats")) {
                return handleStats(userId);
            } else if (event.getPathParameters() != null && event.getPathParameters().containsKey("id")) {
                return handleGetEmail(userId, event.getPathParameters().get("id"));
            } else {
                return handleListEmails(userId);
            }
        } catch (Exception e) {
            return ApiResponse.error(500, e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleListEmails(String userId) {
        List<Email> emails = queryUserEmails(userId);
        return ApiResponse.ok(emails);
    }

    private APIGatewayProxyResponseEvent handleGetEmail(String userId, String emailId) {
        DynamoDbTable<Email> table = DynamoDbUtil.emailsTable();
        Email email = table.getItem(Key.builder().partitionValue(userId).sortValue(emailId).build());
        if (email == null) return ApiResponse.error(404, "Email not found");
        return ApiResponse.ok(email);
    }

    private APIGatewayProxyResponseEvent handleStats(String userId) {
        List<Email> emails = queryUserEmails(userId);

        Map<String, Long> byCategory = emails.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(Email::getCategory, Collectors.counting()));

        Map<String, Long> byPriority = emails.stream()
                .filter(e -> e.getPriority() != null)
                .collect(Collectors.groupingBy(e -> "priority_" + e.getPriority(), Collectors.counting()));

        long total = emails.size();
        long processed = emails.stream().filter(e -> "processed".equals(e.getStatus())).count();
        long unprocessed = total - processed;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("processed", processed);
        stats.put("unprocessed", unprocessed);
        stats.put("byCategory", byCategory);
        stats.put("byPriority", byPriority);

        return ApiResponse.ok(stats);
    }

    private List<Email> queryUserEmails(String userId) {
        DynamoDbTable<Email> table = DynamoDbUtil.emailsTable();
        return table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .stream()
                .flatMap(p -> p.items().stream())
                .collect(Collectors.toList());
    }

    private String extractUserId(APIGatewayProxyRequestEvent event) {
        Map<String, String> headers = event.getHeaders();
        if (headers == null) return null;
        String auth = headers.get("Authorization");
        if (auth == null) auth = headers.get("authorization");
        return JwtUtil.extractUserIdFromHeader(auth);
    }
}
