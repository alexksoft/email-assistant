package com.emailassistant.util;

import com.emailassistant.model.Email;
import com.emailassistant.model.User;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbUtil {
    private static final DynamoDbClient client = DynamoDbClient.create();
    private static final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client).build();

    private static final String USERS_TABLE = System.getenv("USERS_TABLE");
    private static final String EMAILS_TABLE = System.getenv("EMAILS_TABLE");

    public static DynamoDbTable<User> usersTable() {
        return enhancedClient.table(USERS_TABLE != null ? USERS_TABLE : "EmailAssistant_Users",
                TableSchema.fromBean(User.class));
    }

    public static DynamoDbTable<Email> emailsTable() {
        return enhancedClient.table(EMAILS_TABLE != null ? EMAILS_TABLE : "EmailAssistant_Emails",
                TableSchema.fromBean(Email.class));
    }
}
