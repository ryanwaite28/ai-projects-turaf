/// <reference types="cypress" />

describe('Navigation Flow', () => {
  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
    });
  });

  it('should navigate through all main sections', () => {
    // Dashboard
    cy.visit('/dashboard');
    cy.contains('Dashboard').should('be.visible');
    
    // Problems
    cy.contains('Problems').click();
    cy.url().should('include', '/problems');
    cy.contains('Problems').should('be.visible');
    
    // Hypotheses
    cy.contains('Hypotheses').click();
    cy.url().should('include', '/hypotheses');
    cy.contains('Hypotheses').should('be.visible');
    
    // Experiments
    cy.contains('Experiments').click();
    cy.url().should('include', '/experiments');
    cy.contains('Experiments').should('be.visible');
    
    // Metrics
    cy.contains('Metrics').click();
    cy.url().should('include', '/metrics');
    cy.contains('Metrics').should('be.visible');
    
    // Reports
    cy.contains('Reports').click();
    cy.url().should('include', '/reports');
    cy.contains('Reports').should('be.visible');
  });

  it('should highlight active navigation item', () => {
    cy.visit('/experiments');
    cy.get('nav a[href="/experiments"]').should('have.class', 'active');
  });

  it('should display user menu', () => {
    cy.visit('/dashboard');
    cy.getByTestId('user-menu').click();
    cy.contains('Profile').should('be.visible');
    cy.contains('Settings').should('be.visible');
    cy.contains('Logout').should('be.visible');
  });

  it('should navigate to profile page', () => {
    cy.visit('/dashboard');
    cy.getByTestId('user-menu').click();
    cy.contains('Profile').click();
    cy.url().should('include', '/profile');
  });

  it('should handle browser back/forward navigation', () => {
    cy.visit('/dashboard');
    cy.contains('Problems').click();
    cy.url().should('include', '/problems');
    
    cy.go('back');
    cy.url().should('include', '/dashboard');
    
    cy.go('forward');
    cy.url().should('include', '/problems');
  });

  it('should redirect to dashboard after login', () => {
    cy.logout();
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
      cy.url().should('include', '/dashboard');
    });
  });

  it('should redirect to login when accessing protected route without auth', () => {
    cy.logout();
    cy.visit('/experiments');
    cy.url().should('include', '/auth/login');
  });
});
