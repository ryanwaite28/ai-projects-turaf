Feature: User Logout

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }

  Scenario: Successful logout invalidates token
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    
    Given path '/api/v1/auth/logout'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    
    Given path '/api/v1/auth/me'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 401
