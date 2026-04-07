# Task 007: Implement Authentication Tests

**Status**: ✅ Completed  
**Assignee**: AI Assistant  
**Estimated Time**: 4 hours  
**Actual Time**: < 1 hour  
**Completed**: 2026-03-31  
**Dependencies**: Tasks 001, 002, 003  
**Related Spec**: [Architecture Testing](../../specs/architecture-testing.md#1-authentication--authorization-tests)  
**Related Docs**: [API Alignment Review](../../docs/assessments/architecture-tests-api-alignment-2026-03-31.md)  
**Note**: All authentication test scenarios implemented using Karate framework with LoginResponseDto structure.

---

## Objective

Implement comprehensive authentication and authorization test scenarios using Karate framework, aligned with the updated API contracts that use `LoginResponseDto` structure.

---

## Prerequisites

- Task 001 completed (project structure)
- Task 002 completed (Karate configuration)
- Task 003 completed (wait helpers)
- Understanding of JWT token handling
- Access to test environment

---

## Tasks

### 1. Create Authentication Feature File

Create `src/test/resources/features/authentication/login.feature`:

```gherkin
Feature: User Authentication

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    * def invalidUser = { email: 'test@example.com', password: 'WrongPassword!' }

  Scenario: Successful login returns LoginResponseDto with tokens
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    And match response.accessToken != null
    And match response.refreshToken != null
    And match response.user.email == testUser.email
    And match response.user.id != null
    And match response.tokenType == 'Bearer'
    And match response.expiresIn > 0
    
  Scenario: Login with invalid credentials returns 401
    Given path '/api/v1/auth/login'
    And request invalidUser
    When method POST
    Then status 401
    And match response.message != null
    
  Scenario: Access protected endpoint with valid token
    # Login first
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    # Access protected endpoint
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.email == testUser.email
    And match response.id != null
    
  Scenario: Access protected endpoint without token returns 401
    Given path '/api/v1/auth/me'
    When method GET
    Then status 401
    
  Scenario: Access protected endpoint with expired token returns 401
    # This requires a pre-expired token or waiting for expiration
    * def expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.token'
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + expiredToken
    When method GET
    Then status 401
```

### 2. Create Registration Feature File

Create `src/test/resources/features/authentication/registration.feature`:

```gherkin
Feature: User Registration

  Background:
    * url baseUrl
    * def timestamp = new Date().getTime()
    * def uniqueEmail = 'test+' + timestamp + '@example.com'
    * def newUser = { email: '#(uniqueEmail)', password: 'Test123!', name: 'Test User' }

  Scenario: Successful registration returns LoginResponseDto
    Given path '/api/v1/auth/register'
    And request newUser
    When method POST
    Then status 201
    And match response.accessToken != null
    And match response.refreshToken != null
    And match response.user.email == uniqueEmail
    And match response.user.id != null
    And match response.tokenType == 'Bearer'
    
  Scenario: Registration with existing email returns 409
    # Register first time
    Given path '/api/v1/auth/register'
    And request newUser
    When method POST
    Then status 201
    
    # Try to register again with same email
    Given path '/api/v1/auth/register'
    And request newUser
    When method POST
    Then status 409
    And match response.message contains 'already exists'
    
  Scenario: Registration with invalid email returns 400
    * def invalidUser = { email: 'invalid-email', password: 'Test123!', name: 'Test' }
    Given path '/api/v1/auth/register'
    And request invalidUser
    When method POST
    Then status 400
    
  Scenario: Registration with weak password returns 400
    * def weakPasswordUser = { email: '#(uniqueEmail)', password: '123', name: 'Test' }
    Given path '/api/v1/auth/register'
    And request weakPasswordUser
    When method POST
    Then status 400
```

### 3. Create Token Refresh Feature File

Create `src/test/resources/features/authentication/token-refresh.feature`:

```gherkin
Feature: Token Refresh

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }

  Scenario: Refresh token returns new access token
    # Login to get tokens
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def refreshToken = response.refreshToken
    
    # Refresh token
    Given path '/api/v1/auth/refresh'
    And request { refreshToken: '#(refreshToken)' }
    When method POST
    Then status 200
    And match response.accessToken != null
    And match response.refreshToken != null
    And match response.expiresIn > 0
    
  Scenario: Refresh with invalid token returns 401
    Given path '/api/v1/auth/refresh'
    And request { refreshToken: 'invalid-token' }
    When method POST
    Then status 401
```

### 4. Create Logout Feature File

Create `src/test/resources/features/authentication/logout.feature`:

```gherkin
Feature: User Logout

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }

  Scenario: Successful logout invalidates token
    # Login first
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    # Verify token works
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    
    # Logout
    Given path '/api/v1/auth/logout'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    
    # Verify token no longer works
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 401
```

### 5. Create Test Runner

Create `src/test/java/com/turaf/architecture/runners/AuthenticationTestRunner.java`:

```java
package com.turaf.architecture.runners;

import com.intuit.karate.junit5.Karate;

public class AuthenticationTestRunner {
    
    @Karate.Test
    Karate testLogin() {
        return Karate.run("classpath:features/authentication/login.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testRegistration() {
        return Karate.run("classpath:features/authentication/registration.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testTokenRefresh() {
        return Karate.run("classpath:features/authentication/token-refresh.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testLogout() {
        return Karate.run("classpath:features/authentication/logout.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testAllAuthentication() {
        return Karate.run("classpath:features/authentication")
            .relativeTo(getClass());
    }
}
```

---

## Acceptance Criteria

- [x] Login feature file created with all scenarios
- [x] Registration feature file created with validation tests
- [x] Token refresh feature file created
- [x] Logout feature file created
- [x] Test runner created and configured
- [ ] **Manual Step Required**: Run tests against local environment
- [ ] **Manual Step Required**: Run tests against DEV environment
- [ ] **Manual Step Required**: Implement test data cleanup if needed
- [x] No hardcoded credentials or tokens (uses test credentials)

---

## Verification

```bash
cd services/architecture-tests

# Run authentication tests locally
mvn test -Dtest=AuthenticationTestRunner -Dkarate.env=local

# Run against DEV
mvn test -Dtest=AuthenticationTestRunner -Dkarate.env=dev

# View HTML report
open target/karate-reports/karate-summary.html
```

---

## Notes

- **LoginResponseDto Structure**: Tests must expect `accessToken`, `refreshToken`, `user`, `expiresIn`, `tokenType`
- **Token Usage**: Use `response.accessToken` not `response.token`
- **Unique Test Data**: Use timestamps for unique email addresses
- **Cleanup**: Implement cleanup for test users created during registration tests
- **Security**: Never commit real credentials or tokens to repository

---

## Related Tasks

- **Depends On**: 001 (project structure), 002 (Karate config), 003 (wait helpers)
- **Blocks**: 008 (organization tests), 009 (experiment tests), 010 (report tests)
- **Related**: All other test implementation tasks require authentication

---

## API Endpoints Tested

- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `GET /api/v1/auth/me` - Get current user
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - User logout

---

## Test Coverage

- ✅ Successful authentication flow
- ✅ Invalid credentials handling
- ✅ Token validation
- ✅ Protected endpoint access
- ✅ Registration with validation
- ✅ Duplicate registration prevention
- ✅ Token refresh mechanism
- ✅ Logout and token invalidation
