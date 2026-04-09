Feature: User Registration

  Background:
    * url baseUrl
    * def timestamp = new Date().getTime()
    * def uniqueEmail = 'test+' + timestamp + '@example.com'
    * def uniqueUsername = 'testuser_' + timestamp
    * def newUser = { email: '#(uniqueEmail)', password: 'Test123!', username: '#(uniqueUsername)', firstName: 'Test', lastName: 'User', organizationId: 'test-org-001' }

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
    Given path '/api/v1/auth/register'
    And request newUser
    When method POST
    Then status 201
    
    Given path '/api/v1/auth/register'
    And request newUser
    When method POST
    Then status 409
    And match response.message contains 'already exists'
    
  Scenario: Registration with invalid email returns 400
    * def invalidUser = { email: 'invalid-email', password: 'Test123!', username: 'invalid', firstName: 'Test', lastName: 'User', organizationId: 'test-org-001' }
    Given path '/api/v1/auth/register'
    And request invalidUser
    When method POST
    Then status 400
    
  Scenario: Registration with weak password returns 400
    * def weakPasswordUser = { email: '#(uniqueEmail)', password: '123', username: 'weakpw', firstName: 'Test', lastName: 'User', organizationId: 'test-org-001' }
    Given path '/api/v1/auth/register'
    And request weakPasswordUser
    When method POST
    Then status 400
