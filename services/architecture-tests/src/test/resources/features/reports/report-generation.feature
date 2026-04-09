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

  Scenario: Async report generation workflow via Lambda
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
    
    # Complete experiment - triggers ExperimentCompleted event -> EventBridge -> Reporting Lambda
    Given path '/api/v1/experiments', experimentId, 'complete'
    And header Authorization = 'Bearer ' + token
    And request { resultSummary: 'Complete' }
    When method POST
    Then status 200
    
    # Wait for Lambda to process event and generate report (async)
    * def reportReady = waitHelper.waitForCondition(30, function(){ var resp = karate.call('GET', '/api/v1/reports?experimentId=' + experimentId); return resp.length > 0; })
    * match reportReady == true
    
    # Query the generated report
    Given path '/api/v1/reports'
    And param experimentId = experimentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    * def reportId = response[0].id
    And match response[0].status == 'COMPLETED'
    And match response[0].downloadUrl != null
    
  Scenario: Query reports for non-existent experiment
    # Query reports for non-existent experiment should return empty list
    Given path '/api/v1/reports'
    And param experimentId = 'non-existent-id'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response == []
    
  Scenario: Query generated reports by experiment
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
    
    # Start and complete experiment to trigger Lambda report generation
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
    
    # Wait for Lambda to generate report
    * def reportReady = waitHelper.waitForCondition(30, function(){ var resp = karate.call('GET', '/api/v1/reports?experimentId=' + experimentId); return resp.length > 0; })
    * match reportReady == true
    
    # Query generated reports for this experiment
    Given path '/api/v1/reports'
    And param experimentId = experimentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And assert response.length > 0
    And match response[0].format != null
