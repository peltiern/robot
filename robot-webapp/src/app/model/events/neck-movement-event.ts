import {RobotEvent} from './robot-event';

const NECK_MOVEMENT_EVENT_TYPE = 'mouvement-cou';

export class NeckMovementEvent extends RobotEvent {

  positionInclinaison: number;
  positionPanoramique: number;
  positionMonterDescendre: number;

  constructor() {
    super(NECK_MOVEMENT_EVENT_TYPE);
  }
}
