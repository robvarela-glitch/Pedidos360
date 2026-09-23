import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { Profile } from '../profile/profile';

export const routes: Routes = [
    {
        path: 'profile',
        component: Profile,
        canActivate: [MsalGuard]
    }
];
