import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'orders', loadComponent: () => import('./pages/orders/order-list/order-list.component').then(m => m.OrderListComponent) },
      { path: 'orders/new', loadComponent: () => import('./pages/orders/create-order/create-order.component').then(m => m.CreateOrderComponent) },
      { path: 'orders/:id', loadComponent: () => import('./pages/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent) },
      { path: 'products', loadComponent: () => import('./pages/products/product-list/product-list.component').then(m => m.ProductListComponent) },
      { path: 'products/manage', loadComponent: () => import('./pages/products/product-manage/product-manage.component').then(m => m.ProductManageComponent) },
      { path: 'products/:id', loadComponent: () => import('./pages/products/product-detail/product-detail.component').then(m => m.ProductDetailComponent) },
      { path: 'inventory', loadComponent: () => import('./pages/inventory/inventory.component').then(m => m.InventoryComponent) },
      { path: 'payments', loadComponent: () => import('./pages/payments/payment-history.component').then(m => m.PaymentHistoryComponent) },
      { path: 'analytics', loadComponent: () => import('./pages/analytics/analytics.component').then(m => m.AnalyticsComponent) },
      { path: 'notifications', loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'settings', loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent) },
    ]
  },
  { path: '**', redirectTo: '/dashboard' }
];
