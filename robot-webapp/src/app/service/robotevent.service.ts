import {Observable} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {RxStompService} from '@stomp/ng2-stompjs';
import {Injectable} from '@angular/core';
import {RobotEvent} from '../model/robot-event';

export const CONVERSATION_TOPIC = 'conversation';
export const DETECTION_VOCALE_TOPIC = 'detection-vocale';
export const DISPLAY_POSITION_TOPIC = 'display-position';
export const MOUVEMENT_COU_TOPIC = 'mouvement-cou';
export const MOUVEMENT_ROUE_TOPIC = 'mouvement-roue';
export const MOUVEMENT_YEUX_TOPIC = 'mouvement-yeux';
export const PAROLE_TOPIC = 'parole';
export const PLAY_ANIMATION_TOPIC = 'play-animation';
export const PLAY_SOUND_TOPIC = 'play-sound';
export const RECONNAISSANCE_VOCALE_CONTROLE_TOPIC = 'reconnaissance-vocale-controle';
export const RECONNAISSANCE_VOCALE_TOPIC = 'reconnaissance-vocale';
export const STOP_TOPIC = 'stop';
export const ROBOT_EVENT_TOPICS =
  [
    CONVERSATION_TOPIC,
    DETECTION_VOCALE_TOPIC,
    DISPLAY_POSITION_TOPIC,
    MOUVEMENT_COU_TOPIC,
    MOUVEMENT_ROUE_TOPIC,
    MOUVEMENT_YEUX_TOPIC,
    PAROLE_TOPIC,
    PLAY_ANIMATION_TOPIC,
    PLAY_SOUND_TOPIC,
    RECONNAISSANCE_VOCALE_CONTROLE_TOPIC,
    RECONNAISSANCE_VOCALE_TOPIC,
    STOP_TOPIC

  ]
;
/**
 * Service parent permettant d'écouter les messages du Websocket sur un topic particulier.
 */
@Injectable({
  providedIn: 'root'
})
export class RobotEventWebSocketService {

  /** Objet observable permettant de souscrire à l'écoute des messages. */
  private messageObservableMap: Map<string, Observable<Message>> = new Map();

  /** Souscripteurs des messages. */
  private subscribers: Map<string, Array<any>> = new Map();
  private subscriberIndex: Map<string, number> = new Map<string, number>();

  constructor(
    private stompService: RxStompService
  ) {
    this.createMessageObservable();
    this.connect();
  }

  /**
   * Création de l'objet observable permettant de souscrire à l'écoute des messages .
   */
  private createMessageObservable = () => {
    ROBOT_EVENT_TOPICS.forEach(topic => {
      this.subscribers.set(topic, new Array<any>());
      this.subscriberIndex.set(topic, 0);
      this.messageObservableMap.set(topic, new Observable(observer => {
        const subscriberIndex = this.subscriberIndex.get(topic);
        this.subscriberIndex.set(topic, subscriberIndex + 1);
        this.addToSubscribers(topic, {index: subscriberIndex, observer});
        return () => {
          this.removeFromSubscribers(topic, subscriberIndex);
        };
      }));
    });
  };

  /**
   * Ajoute un souscripteur.
   * @param topic le topic à traiter
   * @param subscriber le souscripteur à ajouter
   */
  private addToSubscribers(topic: string, subscriber: any): void {
    this.subscribers.get(topic).push(subscriber);
  }

  /**
   * Supprime le souscripteur situé à un certain index.
   * @param topic le topic à traiter
   * @param index l'index du souscripteur à supprimer
   */
  private removeFromSubscribers(topic: string, index: number): void {
    for (let i = 0; i < this.subscribers.get(topic).length; i++) {
      if (i === index) {
        this.subscribers.get(topic).splice(i, 1);
        break;
      }
    }
  }

  /**
   * Connecte et active la connexion au topic.
   */
  private connect(): void {
    ROBOT_EVENT_TOPICS.forEach(topic => {
      this.stompService.watch('/events/' + topic).subscribe((message: Message) => this.socketListener(topic, message));
    });
  }

  private socketListener(topic: string, message: Message): void {
    this.subscribers.get(topic).forEach(subscriber => {
      subscriber.observer.next(message);
    });
  };

  /**
   * Retourne l'objet observable permettant de souscrire à l'écoute des messages.
   */
  public getMessageObservable(topic: string): Observable<Message> {
    return this.messageObservableMap.get(topic);
  }

  public sendMessage(robotEvent: RobotEvent): void {
    this.stompService.publish({destination: '/app/robotevents', body: JSON.stringify(robotEvent)});
  }
}
