import {RobotEvent} from './robot-event';

const PAROLE_EVENT_TYPE = 'parole';

export class ParoleEvent extends RobotEvent {

  private texte: string;

  constructor(texte) {
    super(PAROLE_EVENT_TYPE);
    this.texte = texte;
  }
}
