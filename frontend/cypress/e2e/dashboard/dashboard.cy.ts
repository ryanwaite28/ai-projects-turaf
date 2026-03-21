/// <reference types="cypress" />

describe('Dashboard Flow', () => {
  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
      cy.visit('/dashboard');
    });
  });

  it('should display dashboard overview', () => {
    cy.contains('Dashboard').should('be.visible');
    cy.getByTestId('stats-grid').should('be.visible');
  });

  it('should display key metrics', () => {
    cy.intercept('GET', '**/dashboard/stats').as('getStats');
    cy.visit('/dashboard');
    cy.wait('@getStats');
    
    cy.getByTestId('total-problems').should('be.visible');
    cy.getByTestId('active-experiments').should('be.visible');
    cy.getByTestId('completed-experiments').should('be.visible');
    cy.getByTestId('pending-hypotheses').should('be.visible');
  });

  it('should display recent activity', () => {
    cy.intercept('GET', '**/dashboard/activity').as('getActivity');
    cy.visit('/dashboard');
    cy.wait('@getActivity');
    
    cy.getByTestId('recent-activity').should('be.visible');
    cy.contains('Recent Activity').should('be.visible');
  });

  it('should navigate to experiments from dashboard card', () => {
    cy.getByTestId('active-experiments-card').click();
    cy.url().should('include', '/experiments');
  });

  it('should navigate to problems from dashboard card', () => {
    cy.getByTestId('total-problems-card').click();
    cy.url().should('include', '/problems');
  });

  it('should refresh dashboard data', () => {
    cy.intercept('GET', '**/dashboard/stats').as('getStats');
    cy.getByTestId('refresh-button').click();
    cy.wait('@getStats');
    cy.contains('Dashboard updated').should('be.visible');
  });

  it('should display charts and visualizations', () => {
    cy.getByTestId('experiments-chart').should('be.visible');
    cy.getByTestId('metrics-chart').should('be.visible');
  });

  it('should filter dashboard by date range', () => {
    cy.intercept('GET', '**/dashboard/stats*').as('getFilteredStats');
    
    cy.getByTestId('date-range-filter').click();
    cy.contains('Last 7 days').click();
    cy.wait('@getFilteredStats');
    
    cy.url().should('include', 'range=7d');
  });
});
