import {RobotEvent} from './robot-event';

const NECK_MOVEMENT_EVENT_TYPE = 'mouvement-cou';

export class NeckMovementEvent extends RobotEvent {

  positionHautBas: number;
  positionGaucheDroite: number;

  constructor() {
    super(NECK_MOVEMENT_EVENT_TYPE);
  }
}
