Feature: Problem Management

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    * def timestamp = new Date().getTime()
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Problem Org', slug: 'prob-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id

  Scenario: Create and list problems
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'User Retention', description: 'Users are churning', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    And match response.id != null
    And match response.title == 'User Retention'
    * def problemId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[?(@.id == problemId)] != []
    
  Scenario: Update problem
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Original Title', description: 'Original description', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/problems', problemId
    And header Authorization = 'Bearer ' + token
    And request { title: 'Updated Title', description: 'Updated description' }
    When method PUT
    Then status 200
    And match response.title == 'Updated Title'
    
  Scenario: Delete problem
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'To Delete', description: 'Will be deleted', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/problems', problemId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
    
    Given path '/api/v1/problems', problemId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 404
