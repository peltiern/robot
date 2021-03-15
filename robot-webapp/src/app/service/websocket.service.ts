import {Observable} from 'rxjs';
import {Message} from '@stomp/stompjs';
import {RxStompService} from '@stomp/ng2-stompjs';

/**
 * Service parent permettant d'écouter les messages du Websocket sur un topic particulier.
 */
export class WebSocketService {

  /** Objet observable permettant de souscrire à l'écoute des messages. */
  private messageObservable: Observable<Message>;

  /** Souscripteurs des messages. */
  private subscribers: Array<any> = [];
  private subscriberIndex = 0;

  constructor(
    private stompService: RxStompService,
    // Topic
    private destination: string
  ) {
    this.createMessageObservable();
    this.connect();
  }

  /**
   * Création de l'objet observable permettant de souscrire à l'écoute des messages .
   */
  private createMessageObservable = () => {
    this.messageObservable = new Observable(observer => {
      const subscriberIndex = this.subscriberIndex++;
      this.addToSubscribers({index: subscriberIndex, observer});
      return () => {
        this.removeFromSubscribers(subscriberIndex);
      };
    });
  };

  /**
   * Ajoute un souscripteur.
   * @param subscriber le souscripteur à ajouter
   */
  private addToSubscribers(subscriber: any): void {
    this.subscribers.push(subscriber);
  }

  /**
   * Supprime le souscripteur situé à un certain index.
   * @param index l'index du souscripteur à supprimer
   */
  private removeFromSubscribers = index => {
    for (let i = 0; i < this.subscribers.length; i++) {
      if (i === index) {
        this.subscribers.splice(i, 1);
        break;
      }
    }
  };

  /**
   * Connecte et active la connexion au topic.
   */
  private connect(): void {
    this.stompService.watch(this.destination).subscribe(this.socketListener);
  }

  private socketListener = message => {
    this.subscribers.forEach(subscriber => {
      subscriber.observer.next(message);
    });
  };

  /**
   * Retourne l'objet observable permettant de souscrire à l'écoute des messages.
   */
  public getMessageObservable(): Observable<Message> {
    return this.messageObservable;
  }
}
