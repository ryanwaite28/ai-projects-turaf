Feature: Token Refresh

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }

  Scenario: Refresh token returns new access token
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def refreshToken = response.refreshToken
    
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
