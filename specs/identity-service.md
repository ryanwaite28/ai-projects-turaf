# Identity Service Specification

**Source**: PROJECT.md (Section 40)

This specification defines the Identity Service, responsible for user authentication, authorization, and user management.

---

## Service Overview

**Purpose**: Manage user authentication, JWT token issuance, and user profile management

**Bounded Context**: Identity and Access Management

**Service Type**: Core microservice (ECS Fargate)

---

## Responsibilities

- User registration and account creation
- User authentication (login)
- JWT token issuance and validation
- JWT token refresh
- Password management and security
- User profile management
- Session management

---

## Technology Stack

**Framework**: Spring Boot 3.x  
**Security**: Spring Security 6.x with JWT  
**Persistence**: Spring Data JPA  
**Database**: PostgreSQL schema `identity_schema` on shared RDS instance  
**Database User**: `identity_user` (schema-scoped permissions)  
**Build Tool**: Maven  
**Java Version**: Java 17  

**Key Dependencies**:
- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `jjwt` (JWT library)
- `postgresql` driver

---

## API Endpoints

### POST /api/v1/auth/register

**Purpose**: Register a new user account

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "organizationId": "org-uuid"
}
```

**Response** (201 Created):
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "createdAt": "ISO-8601",
    "updatedAt": "ISO-8601"
  },
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**Validation**:
- Email must be valid format
- Email must be unique
- Password must meet security requirements (min 8 chars)
- Username required, max 50 characters, must be unique
- First name required, max 50 characters
- Last name required, max 50 characters
- Organization ID required

**Business Rules**:
- Password is hashed using BCrypt before storage
- User is assigned to the specified organization
- Email verification may be required (future enhancement)

---

### POST /api/v1/auth/login

**Purpose**: Authenticate user and issue JWT tokens

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response** (200 OK):
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "createdAt": "ISO-8601",
    "updatedAt": "ISO-8601"
  },
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**Response** (401 Unauthorized):
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}
```

**Business Rules**:
- Access token expires in 15 minutes
- Refresh token expires in 7 days
- Failed login attempts are logged
- Account lockout after 5 failed attempts (future enhancement)

---

### POST /api/v1/auth/refresh

**Purpose**: Refresh access token using refresh token

**Request Body**:
```json
{
  "refreshToken": "jwt-refresh-token"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "new-jwt-access-token",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

**Response** (401 Unauthorized):
```json
{
  "error": "Invalid or expired refresh token"
}
```

**Business Rules**:
- Refresh token must be valid and not expired
- New access token issued with same claims
- Refresh token is not rotated (future enhancement)

---

### GET /api/v1/users/me

**Purpose**: Get current authenticated user profile

**Headers**:
```
X-User-Id: {user-id}
```

**Response** (200 OK):
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

**Response** (404 Not Found):
```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found"
}
```

---

### PUT /api/v1/users/me/password

**Purpose**: Change user password

**Headers**:
```
Authorization: Bearer {access-token}
```

**Request Body**:
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Response** (200 OK):
```json
{
  "message": "Password updated successfully"
}
```

**Response** (400 Bad Request):
```json
{
  "error": "Current password is incorrect"
}
```

**Business Rules**:
- Current password must be verified
- New password must meet security requirements
- New password must be different from current
- All existing refresh tokens are invalidated

---

### POST /api/v1/auth/logout

**Purpose**: Logout user and invalidate tokens

**Headers**:
```
Authorization: Bearer {access-token}
```

**Response** (200 OK):
```json
{
  "message": "Logged out successfully"
}
```

**Business Rules**:
- Refresh token is deleted from database
- Access token remains valid until expiration (stateless)
- Client should discard access token

---

### POST /api/v1/auth/password-reset/request

**Purpose**: Request a password reset token (sent via email)

**Request Body**:
```json
{
  "email": "user@example.com"
}
```

**Response** (200 OK): Always returns success to prevent email enumeration.

**Business Rules**:
- If email exists, a reset token is generated and stored
- Any existing reset tokens for the user are invalidated
- Token expires after 1 hour (configurable)
- In production, the token is sent via email

---

### POST /api/v1/auth/password-reset/confirm

**Purpose**: Confirm password reset with token and new password

**Request Body**:
```json
{
  "token": "reset-token-uuid",
  "newPassword": "NewSecurePassword123!"
}
```

**Response** (200 OK): Password reset successful.

**Response** (401 Unauthorized):
```json
{
  "code": "INVALID_TOKEN",
  "message": "Invalid password reset token"
}
```

**Validation**:
- Token must be valid and not expired
- Token must not have been used already
- New password must meet security requirements (min 8 chars)

**Business Rules**:
- Token is marked as used after successful reset
- Password change event is published

---

## Database Schema

**Schema**: `identity_schema`  
**Connection Configuration**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/turaf?currentSchema=identity_schema
    username: identity_user
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: identity_schema
  flyway:
    schemas: identity_schema
    default-schema: identity_schema
```

### users

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

### user_roles

```sql
CREATE TABLE user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_roles_user_role UNIQUE (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
```

**Roles**: `ADMIN`, `MEMBER`, `VIEWER`

### refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### password_reset_tokens

```sql
CREATE TABLE password_reset_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
```

---

## Security Configuration

### Password Hashing

**Algorithm**: BCrypt  
**Strength**: 12 rounds  

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### JWT Configuration

**Access Token**:
- Algorithm: HS256
- Expiration: 1 hour
- Claims: userId, email, roles, organizationIds

**Refresh Token**:
- Algorithm: HS256
- Expiration: 7 days
- Claims: userId, tokenId

**Signing Key**:
- Stored in AWS Secrets Manager
- Rotated periodically
- Different keys per environment

### CORS Configuration

**Allowed Origins**:
- DEV: `http://localhost:4200`
- QA: `https://app.qa.turafapp.com`
- PROD: `https://app.turafapp.com`

**Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS  
**Allowed Headers**: Authorization, Content-Type  
**Exposed Headers**: Authorization  
**Allow Credentials**: true  

---

## Application Services

### AuthenticationService

**Responsibilities**:
- User registration
- User login
- Token generation
- Token validation

**Methods**:
```java
public interface AuthenticationService {
    UserDto register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}
```

### UserService

**Responsibilities**:
- User profile management
- Password management
- User queries

**Methods**:
```java
public interface UserService {
    UserDto getCurrentUser(UserId userId);
    UserDto updateProfile(UserId userId, UpdateProfileRequest request);
    void changePassword(UserId userId, ChangePasswordRequest request);
    Optional<User> findByEmail(String email);
}
```

### TokenService

**Responsibilities**:
- JWT token generation
- JWT token validation
- Token parsing

**Methods**:
```java
public interface TokenService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    Claims validateToken(String token);
    UserId extractUserId(String token);
}
```

---

## Domain Logic

### User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String name;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    // Business methods
    public void changePassword(String currentPassword, String newPassword, PasswordEncoder encoder) {
        if (!encoder.matches(currentPassword, this.passwordHash)) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        validatePasswordStrength(newPassword);
        this.passwordHash = encoder.encode(newPassword);
    }
    
    private void validatePasswordStrength(String password) {
        // Min 8 chars, uppercase, lowercase, number, special char
    }
}
```

---

## Events Published

**None**: Identity Service does not publish domain events (authentication is synchronous)

---

## Dependencies on Other Services

### Organization Service

**Purpose**: Validate user-organization relationships

**Integration**: REST API call to Organization Service

**Endpoints Used**:
- `GET /api/v1/organizations/{orgId}/members/{userId}` - Verify membership

---

## Error Handling

**Error Response Format**:
```json
{
  "timestamp": "ISO-8601",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "errors": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

**HTTP Status Codes**:
- 200: Success
- 201: Created
- 400: Bad Request (validation errors)
- 401: Unauthorized (invalid credentials or token)
- 403: Forbidden (insufficient permissions)
- 404: Not Found
- 500: Internal Server Error

---

## Validation Rules

### Email Validation
- Must be valid email format
- Must be unique in system
- Case-insensitive

### Password Validation
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character
- Maximum 128 characters

### Name Validation
- Required
- 1-100 characters
- No special validation

---

## Testing Strategy

### Unit Tests
- Test password hashing and validation
- Test JWT token generation and parsing
- Test authentication logic
- Test validation rules

### Integration Tests
- Test database interactions
- Test repository methods
- Test transaction boundaries

### API Tests
- Test all endpoints with MockMvc
- Test authentication flows
- Test error scenarios
- Test validation

---

## Monitoring and Observability

### Metrics to Track
- Login attempts (success/failure)
- Registration count
- Token refresh count
- Password change count
- Average response time per endpoint

### Logging
- Log all authentication attempts
- Log all authorization failures
- Log password changes
- Include correlation IDs

### Health Checks
- `/actuator/health` - Service health
- Database connectivity check
- Secrets Manager connectivity check

---

## Security Considerations

### Threat Mitigation
- **Brute Force**: Rate limiting on login endpoint
- **Token Theft**: Short-lived access tokens
- **CSRF**: Stateless JWT (no cookies)
- **XSS**: HttpOnly cookies for refresh tokens (future)
- **SQL Injection**: Parameterized queries via JPA

### Compliance
- GDPR: Support user data export and deletion
- Password storage: Industry-standard BCrypt
- Audit logging: All authentication events logged

---

## Future Enhancements

- Email verification on registration
- Password reset via email
- Multi-factor authentication (MFA)
- OAuth2 integration (Google, GitHub)
- Account lockout after failed attempts
- Password history (prevent reuse)
- Session management and revocation

---

## References

- PROJECT.md: Identity Service specification
- Spring Security Documentation
- JWT Best Practices
- OWASP Authentication Cheat Sheet
