Feature: Complete Experiment Lifecycle

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
    
    * def timestamp = new Date().getTime()
    * def orgSlug = 'exp-' + timestamp
    Given path '/api/v1/organizations'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Experiment Org', slug: '#(orgSlug)' }
    When method POST
    Then status 201
    * def orgId = response.id

  Scenario: Complete experiment workflow with async report generation
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Test Problem', description: 'Problem description', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def problemId = response.id
    
    Given path '/api/v1/hypotheses'
    And header Authorization = 'Bearer ' + token
    And request { problemId: '#(problemId)', statement: 'If we do X, Y will improve', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def hypothesisId = response.id
    
    Given path '/api/v1/experiments'
    And header Authorization = 'Bearer ' + token
    And request { name: 'Test Experiment', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    Given path '/api/v1/experiments', experimentId, 'start'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    And match response.status == 'RUNNING'
    
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'conversion_rate', value: 0.25 }
    When method POST
    Then status 201
    
    Given path '/api/v1/metrics'
    And header Authorization = 'Bearer ' + token
    And request { experimentId: '#(experimentId)', name: 'user_engagement', value: 0.75 }
    When method POST
    Then status 201
    
    Given path '/api/v1/experiments', experimentId, 'complete'
    And header Authorization = 'Bearer ' + token
    And request { resultSummary: 'Experiment completed successfully' }
    When method POST
    Then status 200
    And match response.status == 'COMPLETED'
    
    * def reportReady = waitHelper.waitForCondition('/api/v1/experiments/' + experimentId + '/report', 30)
    * match reportReady == true
    
    Given path '/api/v1/experiments', experimentId, 'report'
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.reportUrl != null
    And match response.generated == true

  Scenario: Cancel running experiment
    Given path '/api/v1/problems'
    And header Authorization = 'Bearer ' + token
    And request { title: 'Cancel Test', description: 'Test cancellation', organizationId: '#(orgId)' }
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
    And request { name: 'Cancel Experiment', hypothesisId: '#(hypothesisId)', organizationId: '#(orgId)' }
    When method POST
    Then status 201
    * def experimentId = response.id
    
    Given path '/api/v1/experiments', experimentId, 'start'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    And match response.status == 'RUNNING'
    
    Given path '/api/v1/experiments', experimentId, 'cancel'
    And header Authorization = 'Bearer ' + token
    When method POST
    Then status 200
    And match response.status == 'CANCELLED'
    
    Given path '/api/v1/experiments', experimentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.status == 'CANCELLED'
