import {Component, OnDestroy, OnInit} from '@angular/core';
import {VideoService} from '../../service/video.service';
import {DomSanitizer} from '@angular/platform-browser';
import {Message} from '@stomp/stompjs';
import {AudioService} from '../../service/audio.service';
import {decode} from 'base64-arraybuffer';

@Component({
  selector: 'app-screen',
  templateUrl: './screen.component.html',
  styleUrls: ['./screen.component.css']
})
export class ScreenComponent implements OnInit, OnDestroy {

  // Contenu de l'image à afficher
  public imgContent;

  // Abonnement au topic "video" du Websocket
  public videoSubscription;

  // Abonnement au topic "audio" du Websocket
  public audioSubscription;

  private context = new AudioContext();

  constructor(
    private videoService: VideoService,
    private audioService: AudioService,
    private sanitizer: DomSanitizer
  ) {
  }

  ngOnInit(): void {
    // Ecoute du topic "video" du Websocket
    this.videoSubscription = this.videoService.getMessageObservable().subscribe((videoEventMsg: Message) => this.onNewVideoEventMsg(videoEventMsg));
    // Ecoute du topic "audio" du Websocket
    this.audioSubscription = this.audioService.getMessageObservable().subscribe((audioEventMsg: Message) => this.onNewAudioEventMsg(audioEventMsg));
  }

  ngOnDestroy(): void {
    // Arrêt de l'écoute du topic à la destruction du composant (pour éviter des écoutes multiples)
    this.videoSubscription.unsubscribe();
  }

  /**
   * Construit le contenu de l'image à partir du message reçu dans le Websocket en sécurisant l'URL.
   * @param videoEventMsg le message reçu dans le Websocket
   */
  private onNewVideoEventMsg(videoEventMsg: Message): void {
    const videoEvent = JSON.parse(videoEventMsg.body);
    this.imgContent = this.sanitizer.bypassSecurityTrustResourceUrl(`data:image/jpeg;base64, ${videoEvent.imageBase64}`);
  }

  /**
   * Lit la frame audio reçue dans l'évènement.
   * @param audioEventMsg le message reçu dans le Websocket
   */
  private onNewAudioEventMsg(audioEventMsg: Message): void {
    const audioEvent = JSON.parse(audioEventMsg.body);

    // const context = new AudioContext();
    const source = this.context.createBufferSource();
    this.context.decodeAudioData(decode(audioEvent.audioContentBase64)).then((buffer) => {
      source.buffer = buffer;
      source.connect(this.context.destination);
      source.start(this.context.currentTime);
    }).catch((error) => {
      console.log(error); /// error is : DOMException: Unable to decode audio data
    });
  }

}
