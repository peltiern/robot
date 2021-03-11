import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ScreenComponent} from './screen/screen.component';
import {ControlComponent} from './control.component';
import { ChatComponent } from './chat/chat.component';
import {play, NgxBootstrapIconsModule} from 'ngx-bootstrap-icons';
import {FormsModule} from '@angular/forms';

const icons = {
  play,
};

@NgModule({
  declarations: [ScreenComponent, ControlComponent, ChatComponent],
  exports: [
    ScreenComponent,
    ControlComponent,
    ChatComponent,
  ],
  imports: [
    CommonModule,
    NgxBootstrapIconsModule.pick(icons),
    FormsModule
  ]
})
export class ControlModule {
}
