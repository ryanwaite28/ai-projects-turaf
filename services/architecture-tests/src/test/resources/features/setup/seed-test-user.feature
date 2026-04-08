@ignore
Feature: Seed Test User

  Scenario: Register test user (ignores 409 if already exists)
    Given url baseUrl
    And path '/api/v1/auth/register'
    And request testUserPayload
    When method POST
    * def seedStatus = responseStatus == 201 ? 'created' : (responseStatus == 409 ? 'already exists' : 'unexpected: ' + responseStatus)
    * karate.log('Seed test user status:', seedStatus)
