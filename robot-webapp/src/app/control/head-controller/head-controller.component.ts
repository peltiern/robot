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
  readonly UP_BOTTOM_NECK = 'up-bottom';
  readonly LEFT_RIGHT_NECK = 'left-right';

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

  // Cou "Haut-Bas"
  neckUpBottomPosition = 0;
  // Cou "Gauche-Droite"
  neckLeftRightPosition = 0;
// Options des sliders du cou
  neckUpBottomOptions = {
    floor: -35,
    ceil: 35,
    step: 1,
    vertical: true,
    translate: (value: number): string => {
      return value + '°';
    }
  };
  neckLeftRightOptions = {
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
   * @param source "Haut-Bas" ou "Gauche-Droite"
   */
  onNeckPositionChange(changeContext: ChangeContext, source: string): void {
    if (changeContext && source && (source === this.UP_BOTTOM_NECK || source === this.LEFT_RIGHT_NECK)) {
      const neckMovementEvent = new NeckMovementEvent();
      if (source === this.UP_BOTTOM_NECK) {
        neckMovementEvent.positionHautBas = changeContext.value;
      } else {
        neckMovementEvent.positionGaucheDroite = changeContext.value;
      }
      this.robotEventWebSocketService.sendMessage(neckMovementEvent);
    }
  }
}
