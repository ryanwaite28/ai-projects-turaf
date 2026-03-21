/// <reference types="cypress" />

describe('Create Experiment Flow', () => {
  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
      cy.visit('/experiments');
    });
  });

  it('should display experiments list page', () => {
    cy.contains('Experiments').should('be.visible');
    cy.getByTestId('create-experiment-button').should('be.visible');
  });

  it('should navigate to create experiment wizard', () => {
    cy.getByTestId('create-experiment-button').click();
    cy.url().should('include', '/experiments/create');
    cy.contains('Create Experiment').should('be.visible');
  });

  it('should complete experiment creation wizard', () => {
    cy.intercept('GET', '**/problems*').as('getProblems');
    cy.intercept('GET', '**/hypotheses*').as('getHypotheses');
    cy.intercept('POST', '**/experiments').as('createExperiment');
    
    cy.visit('/experiments/create');
    
    // Step 1: Basic Information
    cy.contains('Basic Information').should('be.visible');
    cy.get('input[name="name"]').type('Test Experiment');
    cy.get('textarea[name="description"]').type('This is a test experiment to validate our hypothesis.');
    cy.get('input[name="owner"]').type('Test Team');
    cy.contains('Next').click();
    
    // Step 2: Select Problem
    cy.wait('@getProblems');
    cy.contains('Select Problem').should('be.visible');
    cy.get('[data-testid="problem-card"]').first().click();
    cy.contains('Next').click();
    
    // Step 3: Select Hypothesis
    cy.wait('@getHypotheses');
    cy.contains('Select Hypothesis').should('be.visible');
    cy.get('[data-testid="hypothesis-card"]').first().click();
    cy.contains('Next').click();
    
    // Step 4: Configure Experiment
    cy.contains('Configure Experiment').should('be.visible');
    cy.get('select[name="type"]').select('A_B_TEST');
    cy.get('input[name="duration"]').clear().type('7');
    cy.get('input[name="sampleSize"]').clear().type('1000');
    cy.get('select[name="successCriteria"]').select('CONVERSION_RATE');
    cy.contains('Next').click();
    
    // Step 5: Review and Create
    cy.contains('Review').should('be.visible');
    cy.contains('Test Experiment').should('be.visible');
    cy.contains('Create Experiment').click();
    
    cy.wait('@createExperiment').its('response.statusCode').should('eq', 201);
    
    // Should redirect to experiment detail page
    cy.url().should('match', /\/experiments\/[a-zA-Z0-9-]+$/);
    cy.contains('Test Experiment').should('be.visible');
  });

  it('should navigate back through wizard steps', () => {
    cy.visit('/experiments/create');
    
    // Go to step 2
    cy.get('input[name="name"]').type('Test');
    cy.contains('Next').click();
    
    // Go back to step 1
    cy.contains('Back').click();
    cy.get('input[name="name"]').should('have.value', 'Test');
  });

  it('should validate required fields in each step', () => {
    cy.visit('/experiments/create');
    
    // Try to proceed without filling required fields
    cy.contains('Next').click();
    cy.contains('Name is required').should('be.visible');
    cy.contains('Description is required').should('be.visible');
  });

  it('should save experiment as draft', () => {
    cy.intercept('POST', '**/experiments').as('createExperiment');
    
    cy.visit('/experiments/create');
    
    cy.get('input[name="name"]').type('Draft Experiment');
    cy.get('textarea[name="description"]').type('Draft description');
    cy.contains('Save as Draft').click();
    
    cy.wait('@createExperiment');
    cy.url().should('include', '/experiments');
  });

  it('should cancel experiment creation', () => {
    cy.visit('/experiments/create');
    
    cy.get('input[name="name"]').type('Test');
    cy.contains('Cancel').click();
    
    // Should show confirmation dialog
    cy.contains('Discard changes').should('be.visible');
    cy.contains('Confirm').click();
    
    cy.url().should('eq', Cypress.config().baseUrl + '/experiments');
  });
});
