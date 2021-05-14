import {RobotEvent} from './robot-event';

const EYE_MOVEMENT_EVENT_TYPE = 'mouvement-yeux';

export class EyeMovementEvent extends RobotEvent {

  positionOeilGauche: number;
  positionOeilDroit: number;

  constructor() {
    super(EYE_MOVEMENT_EVENT_TYPE);
  }
}
