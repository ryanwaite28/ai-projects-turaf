Feature: Organization Workflows

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken

  Scenario: Complete organization setup workflow
    * def timestamp = new Date().getTime()
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Workflow Org', slug: 'workflow-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[?(@.id == orgId)] != []
    
    * def member1Email = 'member1+' + timestamp + '@example.com'
    Given path '/api/v1/auth/register'
    And request { email: '#(member1Email)', password: 'Test123!', name: 'Member 1' }
    When method POST
    Then status 201
    * def member1Id = response.user.id
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(member1Id)', role: 'MEMBER' }
    When method POST
    Then status 201
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.length >= 2
    
  Scenario: Organization access control
    * def timestamp = new Date().getTime()
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Private Org', slug: 'private-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    * def user2Email = 'user2+' + timestamp + '@example.com'
    Given path '/api/v1/auth/register'
    And request { email: '#(user2Email)', password: 'Test123!', name: 'User 2' }
    When method POST
    Then status 201
    * def user2Token = response.accessToken
    
    Given path '/api/v1/organizations', orgId
    And header Authorization = 'Bearer ' + user2Token
    When method GET
    Then status 403
