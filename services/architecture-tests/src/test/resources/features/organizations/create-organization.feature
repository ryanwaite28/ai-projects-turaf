Feature: Organization CRUD Operations

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    * def userId = response.user.id

  Scenario: Create organization successfully
    * def timestamp = new Date().getTime()
    * def orgName = 'Test Org ' + timestamp
    * def orgSlug = 'test-org-' + timestamp
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: '#(orgName)', slug: '#(orgSlug)', description: 'Test organization' }
    When method POST
    Then status 201
    And match response.id != null
    And match response.name == orgName
    And match response.slug == orgSlug
    * def orgId = response.id
    
  Scenario: List user's organizations
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response != null
    And match response[*].id != null
    And match response[*].name != null
    
  Scenario: Get organization by ID
    * def timestamp = new Date().getTime()
    * def orgName = 'Test Org ' + timestamp
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: '#(orgName)', slug: 'test-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/organizations', orgId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.id == orgId
    And match response.name == orgName
    
  Scenario: Update organization
    * def timestamp = new Date().getTime()
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Original Name', slug: 'org-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/organizations', orgId
    And header Authorization = 'Bearer ' + token
    And request { name: 'Updated Name' }
    When method PUT
    Then status 200
    And match response.name == 'Updated Name'
    
  Scenario: Delete organization
    * def timestamp = new Date().getTime()
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'To Delete', slug: 'delete-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/organizations', orgId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
    
    Given path '/api/v1/organizations', orgId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 404
