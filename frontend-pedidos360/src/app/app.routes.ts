import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Catalog } from './pages/catalog/catalog';
import { Orders } from './pages/orders/orders';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: Login
  },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard]
  },
  {
    path: 'catalog',
    component: Catalog,
    canActivate: [authGuard]
  },
  {
    path: 'orders',
    component: Orders,
    canActivate: [authGuard]
  }
];