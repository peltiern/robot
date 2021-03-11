import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {gearFill, graphUp, joystick, NgxBootstrapIconsModule} from 'ngx-bootstrap-icons';
import {VideoService} from './service/video.service';
import {InjectableRxStompConfig, RxStompService, rxStompServiceFactory} from '@stomp/ng2-stompjs';
import {websocketRxStompConfig} from './service/websocket-rx-stomp.config';
import {RouterModule} from '@angular/router';
import {AppRoutingModule} from './app-routing.module';
import {DashboardModule} from './dashboard/dashboard.module';
import {ControlModule} from './control/control.module';
import {AudioService} from './service/audio.service';
import {RobotEventWebSocketService} from './service/robotevent.service';

const icons = {
  gearFill,
  joystick,
  graphUp
};

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    NgbModule,
    NgxBootstrapIconsModule.pick(icons),
    RouterModule,
    AppRoutingModule,
    ControlModule,
    DashboardModule,
  ],
  providers: [
    VideoService,
    AudioService,
    RobotEventWebSocketService,
    {
      provide: InjectableRxStompConfig,
      useValue: websocketRxStompConfig,
    },
    {
      provide: RxStompService,
      useFactory: rxStompServiceFactory,
      deps: [InjectableRxStompConfig],
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
