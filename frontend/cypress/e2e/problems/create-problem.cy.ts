/// <reference types="cypress" />

describe('Create Problem Flow', () => {
  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
      cy.visit('/problems');
    });
  });

  it('should display problems list page', () => {
    cy.contains('Problems').should('be.visible');
    cy.getByTestId('create-problem-button').should('be.visible');
  });

  it('should navigate to create problem form', () => {
    cy.getByTestId('create-problem-button').click();
    cy.url().should('include', '/problems/create');
    cy.contains('Create Problem').should('be.visible');
  });

  it('should show validation errors for required fields', () => {
    cy.visit('/problems/create');
    cy.get('button[type="submit"]').click();
    
    cy.contains('Title is required').should('be.visible');
    cy.contains('Description is required').should('be.visible');
  });

  it('should successfully create a problem', () => {
    cy.intercept('POST', '**/problems').as('createProblem');
    
    cy.visit('/problems/create');
    
    // Fill out the form
    cy.get('input[name="title"]').type('Test Problem Title');
    cy.get('textarea[name="description"]').type('This is a detailed description of the test problem.');
    cy.get('select[name="category"]').select('PERFORMANCE');
    cy.get('select[name="priority"]').select('HIGH');
    cy.get('textarea[name="context"]').type('Additional context about the problem');
    
    // Add tags
    cy.get('input[name="tags"]').type('test{enter}');
    cy.get('input[name="tags"]').type('automation{enter}');
    
    // Submit the form
    cy.get('button[type="submit"]').click();
    
    // Wait for API response
    cy.wait('@createProblem').its('response.statusCode').should('eq', 201);
    
    // Should redirect to problem detail page
    cy.url().should('match', /\/problems\/[a-zA-Z0-9-]+$/);
    cy.contains('Test Problem Title').should('be.visible');
    cy.contains('This is a detailed description').should('be.visible');
  });

  it('should cancel problem creation', () => {
    cy.visit('/problems/create');
    
    cy.get('input[name="title"]').type('Test Problem');
    cy.contains('Cancel').click();
    
    cy.url().should('eq', Cypress.config().baseUrl + '/problems');
  });

  it('should handle API errors gracefully', () => {
    cy.intercept('POST', '**/problems', {
      statusCode: 500,
      body: { message: 'Internal server error' }
    }).as('createProblem');
    
    cy.visit('/problems/create');
    
    cy.get('input[name="title"]').type('Test Problem');
    cy.get('textarea[name="description"]').type('Test description');
    cy.get('button[type="submit"]').click();
    
    cy.wait('@createProblem');
    cy.contains('Internal server error').should('be.visible');
  });

  it('should save problem as draft', () => {
    cy.intercept('POST', '**/problems').as('createProblem');
    
    cy.visit('/problems/create');
    
    cy.get('input[name="title"]').type('Draft Problem');
    cy.get('textarea[name="description"]').type('Draft description');
    cy.get('select[name="status"]').select('DRAFT');
    
    cy.contains('Save as Draft').click();
    
    cy.wait('@createProblem');
    cy.url().should('include', '/problems');
    cy.contains('Draft Problem').should('be.visible');
  });
});
