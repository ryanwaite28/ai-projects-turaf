Feature: Report Management

  Background:
    * url baseUrl
    * def waitHelper = Java.type('com.turaf.architecture.helpers.WaitHelper')
    * def testUser = { email: 'test@example.com', password: 'Test123!' }
    
    Given path '/api/v1/auth/login'
    And request testUser
    When method POST
    Then status 200
    * def token = response.accessToken
    
    * def timestamp = new Date().getTime()
    * def orgSlug = 'report-' + timestamp
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Report Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id
    
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Test Problem', description: 'For reports', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'Test hypothesis', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/experiments'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Report Test Experiment', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id

  Scenario: List reports with filters
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def reportId = response.id
    
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response != null
    
    Given path '/api/v1/reports'
    And param type = 'EXPERIMENT'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response[*].type contains only 'EXPERIMENT'
    
    Given path '/api/v1/reports'
    And param status = 'PENDING'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    
  Scenario: Create and download report
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def reportId = response.id
    And match response.type == 'EXPERIMENT'
    And match response.format == 'PDF'
    
    * def reportReady = waitHelper.waitForFieldValue('/api/v1/reports/' + reportId, '$.status', 'COMPLETED', 30)
    * match reportReady == true
    
    Given path '/api/v1/reports', reportId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.id == reportId
    And match response.status == 'COMPLETED'
    And match response.downloadUrl != null
    
    Given path '/api/v1/reports', reportId, 'download'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match responseHeaders['Content-Type'][0] == 'application/pdf'
    And match responseHeaders['Content-Disposition'][0] contains 'attachment'
    And match responseBytes.length > 0
    
  Scenario: Delete report
    Given path '/api/v1/reports'
    And header Authorization = 'Bearer ' + token
    And request { type: 'EXPERIMENT', format: 'PDF', experimentId: '#(experimentId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def reportId = response.id
    
    Given path '/api/v1/reports', reportId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
    
    Given path '/api/v1/reports', reportId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 404
    
  Scenario: Report generation from completed experiment
    Given path '/api/v1/experiments', experimentId, 'start'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.35 }
    When method POST
    Then status 201
    
    Given path '/api/v1/experiments', experimentId, 'complete'
    And header Authorization = 'Bearer ' + token
    And request { resultSummary: 'Test completed' }
    When method POST
    Then status 200
    
    Given path '/api/v1/reports'
    And param experimentId = experimentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
