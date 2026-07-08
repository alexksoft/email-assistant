package com.emailassistant.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class User {
    private String userId;
    private String email;
    private String passwordHash;
    private String name;
    private String gmailAccessToken;
    private String gmailRefreshToken;
    private String outlookAccessToken;
    private String outlookRefreshToken;
    private String connectedProvider; // "gmail" or "outlook"
    private Long createdAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGmailAccessToken() { return gmailAccessToken; }
    public void setGmailAccessToken(String gmailAccessToken) { this.gmailAccessToken = gmailAccessToken; }

    public String getGmailRefreshToken() { return gmailRefreshToken; }
    public void setGmailRefreshToken(String gmailRefreshToken) { this.gmailRefreshToken = gmailRefreshToken; }

    public String getOutlookAccessToken() { return outlookAccessToken; }
    public void setOutlookAccessToken(String outlookAccessToken) { this.outlookAccessToken = outlookAccessToken; }

    public String getOutlookRefreshToken() { return outlookRefreshToken; }
    public void setOutlookRefreshToken(String outlookRefreshToken) { this.outlookRefreshToken = outlookRefreshToken; }

    public String getConnectedProvider() { return connectedProvider; }
    public void setConnectedProvider(String connectedProvider) { this.connectedProvider = connectedProvider; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
