import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {ControlComponent} from './control/control.component';
import {DashboardComponent} from './dashboard/dashboard.component';

const routes: Routes = [
  {path: '', redirectTo: 'control', pathMatch: 'full'},
  {path: 'control', component: ControlComponent},
  {path: 'dashboard', component: DashboardComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
