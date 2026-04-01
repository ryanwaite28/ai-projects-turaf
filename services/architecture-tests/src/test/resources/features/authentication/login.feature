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
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
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
    * def expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.token'
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + expiredToken
    When method GET
    Then status 401
