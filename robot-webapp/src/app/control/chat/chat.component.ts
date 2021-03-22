import {AfterViewInit, Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {Message} from '@stomp/stompjs';
import {CONVERSATION_TOPIC, RobotEventWebSocketService} from '../../service/robotevent.service';
import {ParoleEvent} from '../../model/events/parole-event';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, AfterViewInit {

  @ViewChild('scrollMe') private myScrollContainer: ElementRef;
  @ViewChildren('item') itemElements: QueryList<any>;

  // Abonnement au topic "conversation" du Websocket
  public conversationSubscription;

  title = 'angular-chat';
  username = '';
  messages: Array<any> = [];
  newMessage = '';

  constructor(
    private robotEventWebSocketService: RobotEventWebSocketService
  ) {
  }

  ngOnInit(): void {
    // Ecoute du topic "parole" du Websocket
    this.conversationSubscription = this.robotEventWebSocketService.getMessageObservable(CONVERSATION_TOPIC).subscribe((conversationEventMsg: Message) => this.onNewConversationEventMsg(conversationEventMsg));
    this.scrollToBottom();
  }

  /**
   * Construit le contenu de l'image à partir du message reçu dans le Websocket en sécurisant l'URL.
   * @param conversationEventMsg le message reçu dans le Websocket
   */
  private onNewConversationEventMsg(conversationEventMsg: Message): void {
    const conversationEvent = JSON.parse(conversationEventMsg.body);
    this.messages = [...this.messages, conversationEvent];
    this.scrollToBottom();
  }

  ngAfterViewInit(): void {
    this.itemElements.changes.subscribe(_ => this.onItemElementsChanged());
  }

  private onItemElementsChanged(): void {
    this.scrollToBottom();
  }

  // https://pumpingco.de/blog/automatic-scrolling-only-if-a-user-already-scrolled-the-bottom-of-a-page-in-angular/
  scrollToBottom(): void {
    try {
      // this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
      this.myScrollContainer.nativeElement.scroll({
        top: this.myScrollContainer.nativeElement.scrollHeight,
        left: 0,
        behavior: 'smooth'
      });
    } catch (err) { }
  }

  sendMessage(): void {
    if (this.newMessage.trim() === '') {
      return;
    }

    try {
      this.robotEventWebSocketService.sendMessage(new ParoleEvent(this.newMessage.trim()));
      this.newMessage = '';
    } catch (err) {
      console.log(err);
    }
  }
}
