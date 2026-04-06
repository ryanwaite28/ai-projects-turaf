import { Component } from '@angular/core';

@Component({
  selector: 'app-organizations',
  template: `
    <div class="organizations-container">
      <h1>Organizations</h1>
      <p>Organization management feature coming soon...</p>
    </div>
  `,
  styles: [`
    .organizations-container {
      padding: 2rem;
    }
    
    h1 {
      color: #333;
      margin-bottom: 1rem;
    }
  `]
})
export class OrganizationsComponent { }
