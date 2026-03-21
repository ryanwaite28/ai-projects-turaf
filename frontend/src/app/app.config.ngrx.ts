import { ApplicationConfig, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';

import { routes } from './app.routes';
import { authReducer } from './store/auth/auth.reducer';
import { AuthEffects } from './store/auth/auth.effects';

/**
 * Application Configuration with NgRx Store
 * 
 * This configuration sets up:
 * - Router with defined routes
 * - HTTP client with interceptors
 * - NgRx Store with auth reducer
 * - NgRx Effects for side effects
 * - NgRx DevTools for debugging (dev mode only)
 * 
 * Following Angular 17+ standalone component architecture.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      // withInterceptors([authInterceptor])  // TODO: Add when interceptor is created
    ),
    
    // NgRx Store Configuration
    provideStore({
      auth: authReducer
      // Future reducers will be added here:
      // organizations: organizationReducer,
      // experiments: experimentReducer,
      // metrics: metricsReducer
    }),
    
    // NgRx Effects Configuration
    provideEffects([
      AuthEffects
      // Future effects will be added here
    ]),
    
    // NgRx DevTools (only in development)
    provideStoreDevtools({
      maxAge: 25, // Retains last 25 states
      logOnly: !isDevMode(), // Restrict extension to log-only mode in production
      autoPause: true, // Pauses recording actions and state changes when extension window is not open
      trace: false, // If set to true, will include stack trace for every dispatched action
      traceLimit: 75 // Maximum stack trace frames to be stored (in case trace option is true)
    })
  ]
};
