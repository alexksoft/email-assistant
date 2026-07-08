# AI Email Assistant MVP

AI-powered email assistant SaaS that categorizes emails, prioritizes them, and generates draft replies using Google Gemini.

## Architecture

- **Backend**: Java 17 on AWS Lambda + API Gateway + DynamoDB
- **Frontend**: Angular 19 (standalone components)
- **AI**: Google Gemini 2.0 Flash for categorization and draft generation
- **Email Providers**: Gmail API, Microsoft Graph API (OAuth2)

## Backend Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- AWS SAM CLI
- AWS account with configured credentials

### Configuration

Set these environment variables (or use SAM parameter overrides):

```bash
JWT_SECRET=your-32-char-secret-key-here!!!!!
GEMINI_API_KEY=your-gemini-api-key
GMAIL_CLIENT_ID=your-google-client-id
GMAIL_CLIENT_SECRET=your-google-client-secret
OUTLOOK_CLIENT_ID=your-microsoft-client-id
OUTLOOK_CLIENT_SECRET=your-microsoft-client-secret
OAUTH_REDIRECT_URI=https://your-api.execute-api.us-east-1.amazonaws.com/prod/auth/oauth/callback
```

### Build & Deploy

```bash
cd backend
mvn clean package
sam build
sam deploy --guided
```

## Frontend Setup

```bash
cd frontend/email-assistant-ui
npm install
ng serve
```

Update `src/environments/environment.ts` with your API Gateway URL.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/register | Create account |
| POST | /auth/login | Login |
| GET | /auth/oauth/gmail | Start Gmail OAuth |
| GET | /auth/oauth/outlook | Start Outlook OAuth |
| GET | /auth/oauth/callback | OAuth callback |
| POST | /emails/sync | Fetch & process emails |
| GET | /emails | List processed emails |
| GET | /emails/{id} | Get single email |
| POST | /emails/{id}/process | Process email with AI |
| GET | /dashboard/stats | Dashboard statistics |

## OAuth Setup

### Gmail
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create OAuth 2.0 credentials
3. Add `https://www.googleapis.com/auth/gmail.readonly` scope
4. Set redirect URI to your callback URL

### Outlook
1. Go to [Azure Portal](https://portal.azure.com/) → App registrations
2. Create new registration
3. Add `Mail.Read` and `offline_access` permissions
4. Set redirect URI to your callback URL

## Security Notes

- AI does NOT send emails automatically — only generates drafts
- OAuth tokens stored encrypted in DynamoDB
- JWT-based authentication with 24h expiry
- CORS configured for frontend origin
