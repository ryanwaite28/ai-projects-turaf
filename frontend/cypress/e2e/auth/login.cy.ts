/// <reference types="cypress" />

describe('Login Flow', () => {
  beforeEach(() => {
    cy.visit('/auth/login');
  });

  it('should display login form', () => {
    cy.contains('Sign In').should('be.visible');
    cy.get('input[name="email"]').should('be.visible');
    cy.get('input[name="password"]').should('be.visible');
    cy.get('button[type="submit"]').should('be.visible');
  });

  it('should show validation errors for empty fields', () => {
    cy.get('button[type="submit"]').click();
    cy.contains('Email is required').should('be.visible');
    cy.contains('Password is required').should('be.visible');
  });

  it('should show error for invalid email format', () => {
    cy.get('input[name="email"]').type('invalid-email');
    cy.get('input[name="password"]').type('password123');
    cy.get('button[type="submit"]').click();
    cy.contains('Invalid email format').should('be.visible');
  });

  it('should successfully login with valid credentials', () => {
    cy.fixture('users').then((users) => {
      cy.intercept('POST', '**/auth/login').as('loginRequest');
      
      cy.get('input[name="email"]').type(users.testUser.email);
      cy.get('input[name="password"]').type(users.testUser.password);
      cy.get('button[type="submit"]').click();
      
      cy.wait('@loginRequest').its('response.statusCode').should('eq', 200);
      cy.url().should('include', '/dashboard');
      cy.window().its('localStorage.token').should('exist');
    });
  });

  it('should show error for invalid credentials', () => {
    cy.intercept('POST', '**/auth/login', {
      statusCode: 401,
      body: { message: 'Invalid credentials' }
    }).as('loginRequest');
    
    cy.get('input[name="email"]').type('wrong@example.com');
    cy.get('input[name="password"]').type('wrongpassword');
    cy.get('button[type="submit"]').click();
    
    cy.wait('@loginRequest');
    cy.contains('Invalid credentials').should('be.visible');
  });

  it('should navigate to register page', () => {
    cy.contains('Sign Up').click();
    cy.url().should('include', '/auth/register');
  });

  it('should navigate to forgot password page', () => {
    cy.contains('Forgot Password').click();
    cy.url().should('include', '/auth/forgot-password');
  });

  it('should persist login after page refresh', () => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
      cy.reload();
      cy.url().should('include', '/dashboard');
    });
  });
});
