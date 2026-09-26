import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { roleGuard } from './auth/role.guard';
import { SubmitClaimComponent } from './claimant/submit-claim.component';
import { MyClaimsComponent } from './claimant/my-claims.component';
import { OfficerQueueComponent } from './officer/officer-queue.component';
import { OfficerWorkloadComponent } from './officer/officer-workload.component';
import { ExposureDashboardComponent } from './officer/exposure-dashboard.component';
import { ClaimDetailPageComponent } from './shared/claim-detail-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  {
    path: 'submit-claim',
    component: SubmitClaimComponent,
    canActivate: [roleGuard(['CLAIMANT'])],
  },
  {
    path: 'my-claims',
    component: MyClaimsComponent,
    canActivate: [roleGuard(['CLAIMANT'])],
  },
  {
    path: 'my-claims/:id',
    component: ClaimDetailPageComponent,
    canActivate: [roleGuard(['CLAIMANT'])],
  },

  {
    path: 'officer/queue',
    component: OfficerQueueComponent,
    canActivate: [roleGuard(['OFFICER', 'MANAGER'])],
  },
  {
    path: 'officer/workload',
    component: OfficerWorkloadComponent,
    canActivate: [roleGuard(['OFFICER', 'MANAGER'])],
  },
  {
    path: 'officer/claims/:id',
    component: ClaimDetailPageComponent,
    canActivate: [roleGuard(['OFFICER', 'MANAGER'])],
  },

  {
    path: 'dashboard',
    component: ExposureDashboardComponent,
    canActivate: [roleGuard(['OFFICER', 'MANAGER'])],
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
