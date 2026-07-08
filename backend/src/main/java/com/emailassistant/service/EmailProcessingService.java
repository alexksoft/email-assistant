package com.emailassistant.service;

import com.emailassistant.model.Email;
import com.emailassistant.model.User;
import com.emailassistant.util.DynamoDbUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Map;

public class EmailProcessingService {
    private final GmailService gmailService = new GmailService();
    private final OutlookService outlookService = new OutlookService();
    private final GeminiService geminiService = new GeminiService();

    public List<Email> syncEmails(String userId) throws Exception {
        DynamoDbTable<User> usersTable = DynamoDbUtil.usersTable();
        User user = usersTable.getItem(Key.builder().partitionValue(userId).build());

        if (user == null) throw new RuntimeException("User not found");

        List<Email> emails;
        String provider = user.getConnectedProvider();

        if ("gmail".equals(provider)) {
            emails = gmailService.fetchRecentEmails(userId, user.getGmailAccessToken(), 20);
        } else if ("outlook".equals(provider)) {
            emails = outlookService.fetchRecentEmails(userId, user.getOutlookAccessToken(), 20);
        } else {
            throw new RuntimeException("No email provider connected");
        }

        // Save to DynamoDB
        DynamoDbTable<Email> emailsTable = DynamoDbUtil.emailsTable();
        for (Email email : emails) {
            emailsTable.putItem(email);
        }
        return emails;
    }

    public Email processEmail(String userId, String emailId) throws Exception {
        DynamoDbTable<Email> table = DynamoDbUtil.emailsTable();
        Email email = table.getItem(Key.builder().partitionValue(userId).sortValue(emailId).build());

        if (email == null) throw new RuntimeException("Email not found");

        Map<String, String> aiResult = geminiService.categorizeAndDraft(
                email.getSubject(), email.getFrom(), email.getBody());

        email.setCategory(aiResult.get("category"));
        email.setPriority(Integer.parseInt(aiResult.get("priority")));
        email.setDraftReply(aiResult.get("draftReply"));
        email.setStatus("processed");
        email.setProcessedAt(System.currentTimeMillis());

        table.updateItem(email);
        return email;
    }
}
