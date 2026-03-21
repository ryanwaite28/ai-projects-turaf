/// <reference types="cypress" />

describe('Complete Experiment Flow', () => {
  let experimentId: string;

  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.login(users.testUser.email, users.testUser.password);
    });
  });

  it('should complete full experiment lifecycle', () => {
    // 1. Create experiment
    cy.intercept('POST', '**/experiments').as('createExperiment');
    cy.visit('/experiments/create');
    
    cy.get('input[name="name"]').type('Lifecycle Test Experiment');
    cy.get('textarea[name="description"]').type('Testing complete experiment lifecycle');
    cy.contains('Next').click();
    
    // Select problem and hypothesis (simplified)
    cy.get('[data-testid="problem-card"]').first().click();
    cy.contains('Next').click();
    cy.get('[data-testid="hypothesis-card"]').first().click();
    cy.contains('Next').click();
    
    // Configure
    cy.get('select[name="type"]').select('A_B_TEST');
    cy.contains('Next').click();
    
    // Create
    cy.contains('Create Experiment').click();
    cy.wait('@createExperiment').then((interception) => {
      experimentId = interception.response?.body.id;
      cy.url().should('include', `/experiments/${experimentId}`);
    });
    
    // 2. Start experiment
    cy.intercept('POST', `**/experiments/${experimentId}/start`).as('startExperiment');
    cy.getByTestId('start-experiment-button').click();
    cy.contains('Confirm').click();
    cy.wait('@startExperiment');
    cy.contains('Running').should('be.visible');
    
    // 3. Add metrics
    cy.intercept('POST', '**/metrics').as('addMetric');
    cy.getByTestId('add-metric-button').click();
    
    cy.get('input[name="metricName"]').type('Conversion Rate');
    cy.get('input[name="value"]').type('5.2');
    cy.get('select[name="variant"]').select('A');
    cy.contains('Save Metric').click();
    cy.wait('@addMetric');
    
    // Add another metric for variant B
    cy.getByTestId('add-metric-button').click();
    cy.get('input[name="metricName"]').type('Conversion Rate');
    cy.get('input[name="value"]').type('7.8');
    cy.get('select[name="variant"]').select('B');
    cy.contains('Save Metric').click();
    cy.wait('@addMetric');
    
    // 4. View metrics dashboard
    cy.getByTestId('metrics-tab').click();
    cy.contains('Conversion Rate').should('be.visible');
    cy.contains('5.2').should('be.visible');
    cy.contains('7.8').should('be.visible');
    
    // 5. Complete experiment
    cy.intercept('POST', `**/experiments/${experimentId}/complete`).as('completeExperiment');
    cy.getByTestId('complete-experiment-button').click();
    
    // Select winning variant
    cy.get('select[name="winningVariant"]').select('B');
    cy.get('textarea[name="conclusion"]').type('Variant B showed 50% improvement in conversion rate');
    cy.contains('Complete Experiment').click();
    cy.wait('@completeExperiment');
    
    cy.contains('Completed').should('be.visible');
    cy.contains('Variant B showed 50% improvement').should('be.visible');
  });

  it('should pause and resume experiment', () => {
    cy.intercept('GET', '**/experiments/*').as('getExperiment');
    cy.intercept('POST', '**/experiments/*/pause').as('pauseExperiment');
    cy.intercept('POST', '**/experiments/*/resume').as('resumeExperiment');
    
    // Visit running experiment
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    cy.wait('@getExperiment');
    
    // Pause experiment
    cy.getByTestId('pause-experiment-button').click();
    cy.contains('Confirm').click();
    cy.wait('@pauseExperiment');
    cy.contains('Paused').should('be.visible');
    
    // Resume experiment
    cy.getByTestId('resume-experiment-button').click();
    cy.contains('Confirm').click();
    cy.wait('@resumeExperiment');
    cy.contains('Running').should('be.visible');
  });

  it('should stop experiment early', () => {
    cy.intercept('POST', '**/experiments/*/stop').as('stopExperiment');
    
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    
    cy.getByTestId('stop-experiment-button').click();
    cy.get('textarea[name="reason"]').type('Insufficient data quality');
    cy.contains('Stop Experiment').click();
    cy.wait('@stopExperiment');
    
    cy.contains('Stopped').should('be.visible');
  });

  it('should generate experiment report', () => {
    cy.intercept('POST', '**/reports').as('generateReport');
    
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    
    cy.getByTestId('generate-report-button').click();
    cy.get('select[name="reportType"]').select('EXPERIMENT_SUMMARY');
    cy.get('select[name="format"]').select('PDF');
    cy.contains('Generate Report').click();
    cy.wait('@generateReport');
    
    cy.contains('Report generated successfully').should('be.visible');
  });

  it('should export experiment data', () => {
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    
    cy.getByTestId('export-data-button').click();
    cy.get('select[name="format"]').select('CSV');
    cy.contains('Export').click();
    
    // Verify download initiated
    cy.readFile('cypress/downloads/experiment-data.csv').should('exist');
  });

  it('should display experiment timeline', () => {
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    
    cy.getByTestId('timeline-tab').click();
    cy.contains('Experiment Timeline').should('be.visible');
    cy.contains('Created').should('be.visible');
    cy.contains('Started').should('be.visible');
  });

  it('should handle concurrent metric updates', () => {
    cy.intercept('POST', '**/metrics').as('addMetric');
    
    cy.visit('/experiments');
    cy.get('[data-testid="experiment-card"]').first().click();
    
    // Add multiple metrics quickly
    for (let i = 0; i < 3; i++) {
      cy.getByTestId('add-metric-button').click();
      cy.get('input[name="metricName"]').type(`Metric ${i}`);
      cy.get('input[name="value"]').type(`${i * 10}`);
      cy.contains('Save Metric').click();
    }
    
    // All metrics should be saved
    cy.get('@addMetric.all').should('have.length', 3);
  });
});
