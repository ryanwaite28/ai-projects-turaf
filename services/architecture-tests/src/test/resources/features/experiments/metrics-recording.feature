Feature: Metrics Recording

  Background:
    * url baseUrl
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    * def timestamp = new Date().getTime()
    * def orgSlug = 'metrics-' + timestamp
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Metrics Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Test', description: 'Test', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Test', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/experiments'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Metrics Test', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id

  Scenario: Record and retrieve metrics
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.42 }
    When method POST
    Then status 201
    And match response.id != null
    * def metricId = response.id
    
    Given path '/api/v1/metrics/experiments', experimentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[?(@.id == metricId)] != []
    
  Scenario: Delete metric
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'test_metric', value: 1.0 }
    When method POST
    Then status 201
    * def metricId = response.id
    
    Given path '/api/v1/metrics', metricId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
