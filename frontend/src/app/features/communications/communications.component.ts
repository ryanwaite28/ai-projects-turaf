import { Component } from '@angular/core';

@Component({
  selector: 'app-communications',
  template: `
    <div class="communications-container">
      <h1>Communications</h1>
      <p>Messaging and conversations feature coming soon...</p>
    </div>
  `,
  styles: [`
    .communications-container {
      padding: 2rem;
    }
    
    h1 {
      color: #333;
      margin-bottom: 1rem;
    }
  `]
})
export class CommunicationsComponent { }
