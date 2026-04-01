import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-2xl mx-auto">
      <h2 class="text-2xl font-bold mb-6">Settings &amp; Profile</h2>
      <div class="card mb-6">
        <h3 class="text-lg font-semibold mb-4">Profile Information</h3>
        <div class="flex items-center gap-4 mb-6">
          <div class="h-16 w-16 rounded-full bg-primary flex items-center justify-center text-white text-2xl font-bold">
            {{ authService.currentUser()?.firstName?.charAt(0) }}{{ authService.currentUser()?.lastName?.charAt(0) }}
          </div>
          <div>
            <p class="font-semibold text-lg">{{ authService.currentUser()?.firstName }} {{ authService.currentUser()?.lastName }}</p>
            <p class="text-gray-500">{{ authService.currentUser()?.email }}</p>
            <span class="badge badge-info">{{ authService.currentUser()?.role }}</span>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div><label class="block text-sm font-medium mb-1">First Name</label><input type="text" [value]="authService.currentUser()?.firstName || ''" class="form-input" readonly></div>
          <div><label class="block text-sm font-medium mb-1">Last Name</label><input type="text" [value]="authService.currentUser()?.lastName || ''" class="form-input" readonly></div>
          <div class="col-span-2"><label class="block text-sm font-medium mb-1">Email</label><input type="email" [value]="authService.currentUser()?.email || ''" class="form-input" readonly></div>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4">Account</h3>
        <button (click)="authService.logout()" class="btn-danger">Sign Out</button>
      </div>
    </div>
  `
})
export class SettingsComponent {
  authService = inject(AuthService);
}
