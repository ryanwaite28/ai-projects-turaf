import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

/**
 * Auth Routing Module
 * 
 * Defines routes for authentication feature module.
 * 
 * Routes:
 * - /auth/login - Login page
 * - /auth/register - Registration page
 * - /auth (default) - Redirects to login
 */
const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent,
    data: { title: 'Sign In' }
  },
  {
    path: 'register',
    component: RegisterComponent,
    data: { title: 'Create Account' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule { }
