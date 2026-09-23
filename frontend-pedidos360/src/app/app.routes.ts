import { Routes } from '@angular/router';

import { MsalGuard } from '@azure/msal-angular';

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
    canActivate: [MsalGuard]
  },

  {
    path: 'catalog',
    component: Catalog,
    canActivate: [MsalGuard]
  },

  {
    path: 'catalog/new',
    component: NewProduct,
    canActivate: [MsalGuard]
  },

  {
    path: 'catalog/:id/edit',
    component: EditProduct,
    canActivate: [MsalGuard]
  },

  {
    path: 'catalog/:id',
    component: ProductDetail,
    canActivate: [MsalGuard]
  },

  {
    path: 'orders',
    component: Orders,
    canActivate: [MsalGuard]
  },

  {
    path: 'orders/new',
    component: NewOrder,
    canActivate: [MsalGuard]
  },

  {
    path: 'orders/:id',
    component: OrderDetail,
    canActivate: [MsalGuard]
  },

  {
    path: 'reports',
    component: Reports,
    canActivate: [MsalGuard]
  },

  {
    path: 'audit',
    component: Audit,
    canActivate: [MsalGuard]
  }

];