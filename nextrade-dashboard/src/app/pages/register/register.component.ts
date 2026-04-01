import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div class="max-w-md w-full">
        <div class="text-center mb-8">
          <h1 class="text-4xl font-bold text-primary">NexTrade</h1>
          <p class="text-gray-600 mt-2">Create your account</p>
        </div>
        <div class="card">
          @if (error()) {
            <div class="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">{{ error() }}</div>
          }
          <form (ngSubmit)="onSubmit()">
            <div class="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                <input type="text" [(ngModel)]="form.firstName" name="firstName" required class="form-input">
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                <input type="text" [(ngModel)]="form.lastName" name="lastName" required class="form-input">
              </div>
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input type="email" [(ngModel)]="form.email" name="email" required class="form-input">
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <input type="password" [(ngModel)]="form.password" name="password" required class="form-input">
            </div>
            <div class="mb-6">
              <label class="block text-sm font-medium text-gray-700 mb-1">Role</label>
              <select [(ngModel)]="form.role" name="role" class="form-input">
                <option value="CUSTOMER">Customer</option>
                <option value="VENDOR">Vendor</option>
              </select>
            </div>
            <button type="submit" [disabled]="loading()" class="w-full btn-primary py-2.5">
              {{ loading() ? 'Creating account...' : 'Create Account' }}
            </button>
          </form>
          <p class="mt-4 text-center text-sm text-gray-600">
            Already have an account? <a routerLink="/login" class="text-primary font-medium hover:underline">Sign in</a>
          </p>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  form = { email: '', password: '', firstName: '', lastName: '', role: 'CUSTOMER' };
  loading = signal(false);
  error = signal('');
  constructor(private authService: AuthService, private router: Router) {}
  onSubmit() {
    this.loading.set(true);
    this.error.set('');
    this.authService.register(this.form).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => { this.error.set(err.error?.message || 'Registration failed'); this.loading.set(false); }
    });
  }
}
