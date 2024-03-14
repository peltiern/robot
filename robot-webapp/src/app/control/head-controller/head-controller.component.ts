import { Component, OnInit } from '@angular/core';
import {RobotEventWebSocketService} from '../../service/robotevent.service';
import {ChangeContext, Options, PointerType} from '@angular-slider/ngx-slider';
import {EyeMovementEvent} from '../../model/events/eye-movement-event';
import {NeckMovementEvent} from '../../model/events/neck-movement-event';

@Component({
  selector: 'app-head-controller',
  templateUrl: './head-controller.component.html',
  styleUrls: ['./head-controller.component.css']
})
export class HeadControllerComponent implements OnInit {

  readonly LEFT_EYE = 'left';
  readonly RIGHT_EYE = 'right';
  readonly TILT_NECK = 'tilt';
  readonly PAN_NECK = 'pan';

  // Oeil gauche
  leftEyePosition = 0;
  // Oeil droit
  rightEyePosition = 0;
  // Options des sliders des yeux
  eyeOptions = {
    floor: -24,
    ceil: 20,
    step: 1,
    vertical: true,
    translate: (value: number): string => {
      return value + '°';
    }
  };

  // Cou "Inclinaison"
  neckTiltPosition = 0;
  // Cou "Panoramique"
  neckPanPosition = 0;
// Options des sliders du cou
  neckTiltOptions = {
    floor: -35,
    ceil: 35,
    step: 1,
    vertical: true,
    translate: (value: number): string => {
      return value + '°';
    }
  };
  neckPanOptions = {
    floor: -65,
    ceil: 65,
    step: 1,
    translate: (value: number): string => {
      return value + '°';
    }
  };

  constructor(
    private robotEventWebSocketService: RobotEventWebSocketService
  ) { }

  ngOnInit(): void {
  }

  /**
   * Envoie un évènement de mouvement des yeux dans le websocket.
   * @param changeContext l'évènement de changement de valeur du slider
   * @param source l'oeil gauche ou l'oeil droit
   */
  onEyePositionChange(changeContext: ChangeContext, source: string): void {
    if (changeContext && source && (source === this.LEFT_EYE || source === this.RIGHT_EYE)) {
      const eyeMovementEvent = new EyeMovementEvent();
      if (source === this.LEFT_EYE) {
        eyeMovementEvent.positionOeilGauche = changeContext.value;
      } else {
        eyeMovementEvent.positionOeilDroit = changeContext.value;
      }
      this.robotEventWebSocketService.sendMessage(eyeMovementEvent);
    }
  }

  /**
   * Envoie un évènement de mouvement du cou dans le websocket.
   * @param changeContext l'évènement de changement de valeur du slider
   * @param source "Inclinaison" ou "Panoramique"
   */
  onNeckPositionChange(changeContext: ChangeContext, source: string): void {
    if (changeContext && source && (source === this.TILT_NECK || source === this.PAN_NECK)) {
      const neckMovementEvent = new NeckMovementEvent();
      if (source === this.TILT_NECK) {
        neckMovementEvent.positionInclinaison = changeContext.value;
      } else {
        neckMovementEvent.positionPanoramique = changeContext.value;
      }
      this.robotEventWebSocketService.sendMessage(neckMovementEvent);
    }
  }
}
