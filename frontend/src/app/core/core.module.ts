import { NgModule, Optional, SkipSelf } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { AuthInterceptor } from './interceptors/auth.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';

/**
 * Core Module
 * 
 * This module contains singleton services, guards, and interceptors
 * that should only be imported once in the application.
 * 
 * Following Angular best practices:
 * - Singleton services provided in root
 * - HTTP interceptors configured
 * - Guards for route protection
 * - Import guard to prevent multiple imports
 * 
 * This module should only be imported in AppModule.
 */
@NgModule({
  declarations: [],
  imports: [
    CommonModule
  ],
  providers: [
    // HTTP Interceptors
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ]
})
export class CoreModule {
  /**
   * Constructor with import guard
   * 
   * Prevents CoreModule from being imported more than once.
   * Throws an error if CoreModule is imported in a module other than AppModule.
   * 
   * @param parentModule The parent module (should be null)
   */
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throw new Error(
        'CoreModule is already loaded. Import it in the AppModule only.'
      );
    }
  }
}
