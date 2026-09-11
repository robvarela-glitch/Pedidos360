import { Routes } from '@angular/router';

import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Catalog } from './pages/catalog/catalog';
import { Orders } from './pages/orders/orders';
import { Reports } from './pages/reports/reports';
import { Audit } from './pages/audit/audit';

import { ProductDetail } from './pages/product-detail/product-detail';
import { EditProduct } from './pages/edit-product/edit-product';
import { NewProduct } from './pages/new-product/new-product';

import { NewOrder } from './pages/new-order/new-order';
import { OrderDetail } from './pages/order-detail/order-detail';

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
    path: 'catalog/new',
    component: NewProduct,
    canActivate: [authGuard]
  },

  {
    path: 'catalog/:id/edit',
    component: EditProduct,
    canActivate: [authGuard]
  },

  {
    path: 'catalog/:id',
    component: ProductDetail,
    canActivate: [authGuard]
  },

  {
    path: 'orders',
    component: Orders,
    canActivate: [authGuard]
  },

  {
    path: 'orders/new',
    component: NewOrder,
    canActivate: [authGuard]
  },

  {
    path: 'orders/:id',
    component: OrderDetail,
    canActivate: [authGuard]
  },

  {
    path: 'reports',
    component: Reports,
    canActivate: [authGuard]
  },

  {
    path: 'audit',
    component: Audit,
    canActivate: [authGuard]
  }

];