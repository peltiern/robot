import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ScreenComponent} from './screen/screen.component';
import {ControlComponent} from './control.component';
import { ChatComponent } from './chat/chat.component';
import {play, NgxBootstrapIconsModule} from 'ngx-bootstrap-icons';
import {FormsModule} from '@angular/forms';
import { HeadControllerComponent } from './head-controller/head-controller.component';
import {NgxSliderModule} from '@angular-slider/ngx-slider';

const icons = {
  play,
};

@NgModule({
  declarations: [ScreenComponent, ControlComponent, ChatComponent, HeadControllerComponent],
  exports: [
    ScreenComponent,
    ControlComponent,
    ChatComponent,
  ],
  imports: [
    CommonModule,
    NgxBootstrapIconsModule.pick(icons),
    FormsModule,
    NgxSliderModule
  ]
})
export class ControlModule {
}
