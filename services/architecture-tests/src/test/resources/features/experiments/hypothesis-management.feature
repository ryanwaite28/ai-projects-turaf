Feature: Hypothesis Management

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
    And request { name: 'Hypothesis Org', slug: 'hyp-' + timestamp }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Test Problem', description: 'For hypotheses', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id

  Scenario: Create and list hypotheses
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'If X then Y', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    And match response.id != null
    And match response.statement == 'If X then Y'
    * def hypothesisId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[?(@.id == hypothesisId)] != []
    
  Scenario: Filter hypotheses by problem
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Hypothesis 1', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Hypothesis 2', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    
    Given path '/api/v1/hypotheses'
    And param problemId = problemId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.length >= 2
    And match response[*].problemId contains only problemId
    
  Scenario: Update hypothesis
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Original statement', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/hypotheses', hypothesisId
    And header Authorization = 'Bearer ' + token
    And request { statement: 'Updated statement', expectedOutcome: 'Better results' }
    When method PUT
    Then status 200
    And match response.statement == 'Updated statement'
    
  Scenario: Delete hypothesis
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'To delete', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/hypotheses', hypothesisId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
