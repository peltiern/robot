import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {DashboardComponent} from './dashboard.component';
import {TemperatureComponent} from './temperature/temperature.component';
import {ChartsModule} from 'ng2-charts';


@NgModule({
  declarations: [DashboardComponent, TemperatureComponent],
  imports: [
    CommonModule,
    ChartsModule
  ]
})
export class DashboardModule {
}
