import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

import { roleGuard } from './core/guards/role.guard';
import { homeRedirectGuard } from './core/guards/home.guard';
import { Login } from './pages/login/login';
import { Access } from './pages/access/access';
import { Dashboard } from './pages/dashboard/dashboard';
import { Catalog } from './pages/catalog/catalog';
import { Orders } from './pages/orders/orders';
import { Reports } from './pages/reports/reports';
import { Audit } from './pages/audit/audit';
import { ProductDetail } from './pages/product-detail/product-detail';
import { EditProduct } from './pages/edit-product/edit-product';
import { NewProduct } from './pages/new-product/new-product';
import { Profile } from './profile/profile';
import { NewOrder } from './pages/new-order/new-order';
import { OrderDetail } from './pages/order-detail/order-detail';
import { Messaging } from './pages/messaging/messaging';
import { Operations } from './pages/operations/operations';
import { StoreLayout } from './pages/store/store-layout/store-layout';
import { StoreHome } from './pages/store/store-home/store-home';
import { StoreProduct } from './pages/store/store-product/store-product';
import { StoreCart } from './pages/store/store-cart/store-cart';
import { StoreOrders } from './pages/store/store-orders/store-orders';
import { StoreOrderDetail } from './pages/store/store-order-detail/store-order-detail';

/**
 * Cada rol tiene su propia experiencia (MsalGuard exige sesión; roleGuard el rol):
 *   Cliente  -> /tienda     (tienda, carrito, mis pedidos y seguimiento)
 *   Operador -> /operacion  (bandeja de pedidos, detalle, catálogo de consulta)
 *   Admin    -> /dashboard  (panel, reportes, auditoría, mensajería, catálogo; puede ver todo)
 * /inicio redirige a la página de inicio del rol que el backend leyó del token.
 * La autorización real la vuelve a aplicar el backend en cada petición.
 */
const CLIENTE = roleGuard(['Cliente', 'Admin']);
const GESTION = roleGuard(['Operador', 'Admin']);
const ADMIN = roleGuard(['Admin']);

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'inicio', component: Access, canActivate: [MsalGuard, homeRedirectGuard] },
  { path: 'acceso', component: Access, canActivate: [MsalGuard] },
  { path: 'profile', component: Profile, canActivate: [MsalGuard] },

  // ---- Cliente: tienda ----
  {
    path: 'tienda',
    component: StoreLayout,
    canActivate: [MsalGuard, CLIENTE],
    children: [
      { path: '', component: StoreHome, title: 'Tienda | Pedidos360' },
      { path: 'producto/:id', component: StoreProduct, title: 'Producto | Pedidos360' },
      { path: 'carrito', component: StoreCart, title: 'Carrito | Pedidos360' },
      { path: 'pedidos', component: StoreOrders, title: 'Mis pedidos | Pedidos360' },
      { path: 'pedidos/:id', component: StoreOrderDetail, title: 'Seguimiento | Pedidos360' }
    ]
  },

  // ---- Operador (y Admin): gestión de pedidos ----
  { path: 'operacion', component: Operations, canActivate: [MsalGuard, GESTION], title: 'Bandeja | Pedidos360' },
  { path: 'orders', component: Orders, canActivate: [MsalGuard, GESTION] },
  { path: 'orders/new', component: NewOrder, canActivate: [MsalGuard, GESTION] },
  { path: 'orders/:id', component: OrderDetail, canActivate: [MsalGuard, GESTION] },
  { path: 'catalog', component: Catalog, canActivate: [MsalGuard, GESTION] },
  { path: 'catalog/new', component: NewProduct, canActivate: [MsalGuard, ADMIN] },
  { path: 'catalog/:id/edit', component: EditProduct, canActivate: [MsalGuard, ADMIN] },
  { path: 'catalog/:id', component: ProductDetail, canActivate: [MsalGuard, GESTION] },

  // ---- Administrador ----
  { path: 'dashboard', component: Dashboard, canActivate: [MsalGuard, ADMIN], title: 'Panel | Pedidos360' },
  { path: 'reports', component: Reports, canActivate: [MsalGuard, ADMIN] },
  { path: 'audit', component: Audit, canActivate: [MsalGuard, ADMIN] },
  { path: 'messaging', component: Messaging, canActivate: [MsalGuard, ADMIN] },

  { path: '', redirectTo: 'inicio', pathMatch: 'full' },
  { path: '**', redirectTo: 'inicio' }
];
