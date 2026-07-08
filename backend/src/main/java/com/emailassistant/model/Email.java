package com.emailassistant.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class Email {
    private String userId;
    private String emailId;
    private String subject;
    private String from;
    private String to;
    private String body;
    private String snippet;
    private String category;      // "urgent", "sales", "support", "newsletter", "personal", "other"
    private Integer priority;     // 1-5, 1=highest
    private String draftReply;
    private String status;        // "unprocessed", "processed", "approved"
    private Long receivedAt;
    private Long processedAt;
    private String provider;      // "gmail" or "outlook"

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbSortKey
    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getDraftReply() { return draftReply; }
    public void setDraftReply(String draftReply) { this.draftReply = draftReply; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Long receivedAt) { this.receivedAt = receivedAt; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
