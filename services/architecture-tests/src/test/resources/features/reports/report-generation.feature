Feature: Report Generation

  Background:
    * url baseUrl
    * def waitHelper = Java.type('com.turaf.architecture.helpers.WaitHelper')
    * def awsHelper = Java.type('com.turaf.architecture.helpers.AwsHelper')
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken

  Scenario: Async report generation workflow
    * def timestamp = new Date().getTime()
    * def orgSlug = 'gen-' + timestamp
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Gen Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Gen Problem', description: 'For generation', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Gen hypothesis', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/experiments'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Gen Experiment', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    Given path '/api/v1/experiments', experimentId, 'start'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'test_metric', value: 1.0 }
    When method POST
    Then status 201
    
    Given path '/api/v1/experiments', experimentId, 'complete'
    And header Authorization = 'Bearer ' + token
    And request { resultSummary: 'Complete' }
    When method POST
    Then status 200
    
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def reportId = response.id
    And match response.status == 'PENDING'
    
    * def reportReady = waitHelper.waitForFieldValue('/api/v1/reports/' + reportId, '$.status', 'COMPLETED', 30)
    * match reportReady == true
    
    Given path '/api/v1/reports', reportId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.status == 'COMPLETED'
    And match response.downloadUrl != null
    
  Scenario: Report generation failure handling
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: 'non-existent-id' }
    When method POST
    Then status 404
    
  Scenario: Multiple report formats
    * def timestamp = new Date().getTime()
    * def orgSlug = 'format-' + timestamp
    
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Format Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Format Problem', description: 'For formats', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Format hypothesis', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/experiments'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Format Experiment', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    And match response.format == 'PDF'
    
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'CSV', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    And match response.format == 'CSV'
