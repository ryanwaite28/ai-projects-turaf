Feature: Organization Member Management

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    * def userId = response.user.id
    
    * def timestamp = new Date().getTime()
    * def orgSlug = 'member-test-' + timestamp
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Member Test Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id

  Scenario: List organization members
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response != null
    And match response[*].userId != null
    
  Scenario: Add member to organization
    * def newUserEmail = 'newmember+' + timestamp + '@example.com'
    * def newMemberUsername = 'newmember_' + timestamp
    Given path '/api/v1/auth/register'
    And request { email: '#(newUserEmail)', password: 'Test123!', username: '#(newMemberUsername)', firstName: 'New', lastName: 'Member', organizationId: 'test-org-001' }
    When method POST
    Then status 201
    * def newUserId = response.user.id
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(newUserId)', role: 'MEMBER' }
    When method POST
    Then status 201
    And match response.userId == newUserId
    And match response.role == 'MEMBER'
    * def memberId = response.id
    
  Scenario: Update member role
    * def newUserEmail = 'member+' + timestamp + '@example.com'
    * def memberUsername = 'member_' + timestamp
    Given path '/api/v1/auth/register'
    And request { email: '#(newUserEmail)', password: 'Test123!', username: '#(memberUsername)', firstName: 'Member', lastName: 'User', organizationId: 'test-org-001' }
    When method POST
    Then status 201
    * def newUserId = response.user.id
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(newUserId)', role: 'MEMBER' }
    When method POST
    Then status 201
    * def memberId = response.id
    
    Given path '/api/v1/organizations', orgId, 'members', memberId
    And header Authorization = 'Bearer ' + token
    And request { role: 'ADMIN' }
    When method PATCH
    Then status 200
    And match response.role == 'ADMIN'
    
  Scenario: Remove member from organization
    * def newUserEmail = 'removeme+' + timestamp + '@example.com'
    * def removeMeUsername = 'removeme_' + timestamp
    Given path '/api/v1/auth/register'
    And request { email: '#(newUserEmail)', password: 'Test123!', username: '#(removeMeUsername)', firstName: 'Remove', lastName: 'Me', organizationId: 'test-org-001' }
    When method POST
    Then status 201
    * def newUserId = response.user.id
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(newUserId)', role: 'MEMBER' }
    When method POST
    Then status 201
    * def memberId = response.id
    
    Given path '/api/v1/organizations', orgId, 'members', memberId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[?(@.id == memberId)] == []
    
  Scenario: Adding member triggers notification event
    * def newUserEmail = 'notify+' + timestamp + '@example.com'
    * def notifyUsername = 'notify_' + timestamp
    Given path '/api/v1/auth/register'
    And request { email: '#(newUserEmail)', password: 'Test123!', username: '#(notifyUsername)', firstName: 'Notify', lastName: 'Me', organizationId: 'test-org-001' }
    When method POST
    Then status 201
    * def newUserId = response.user.id
    
    Given path '/api/v1/organizations', orgId, 'members'
    And header Authorization = 'Bearer ' + token
    And request { userId: '#(newUserId)', role: 'MEMBER' }
    When method POST
    Then status 201
    
    * def helper = Java.type('com.turaf.architecture.helpers.EventHelper')
    * helper.waitForEventProcessing('MemberAdded', 10)
