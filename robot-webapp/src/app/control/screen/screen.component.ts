import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {VideoService} from '../../service/video.service';
import {DomSanitizer} from '@angular/platform-browser';
import {Message} from '@stomp/stompjs';
import {AudioService} from '../../service/audio.service';
import {decode} from 'base64-arraybuffer';
import {VideoEvent} from '../../model/events/video-event';
import {plainToClass} from 'class-transformer';
import {DetectedObject} from '../../model/object-detection/detected-object';
import {Point2d} from '../../model/object-detection/point2d';

@Component({
  selector: 'app-screen',
  templateUrl: './screen.component.html',
  styleUrls: ['./screen.component.css']
})
export class ScreenComponent implements OnInit, OnDestroy {

  // Contenu de l'image à afficher
  public imgContent;

  // Dimensions originales de l'image
  public imageWidth = 0;
  public imageHeight = 0;

  // Evènement courant (image + détection)
  public currentEvent: VideoEvent;

  // Flags d'affichage des différentes détections
  public showFaces = false;
  public showLandmarks = false;
  public showObjects = false;

  // Abonnement au topic "video" du Websocket
  public videoSubscription;

  // Abonnement au topic "audio" du Websocket
  public audioSubscription;

  // Contexte audio
  private context = new AudioContext();

  @ViewChild('image') private imageElement: ElementRef;

  constructor(
    private videoService: VideoService,
    private audioService: AudioService,
    private sanitizer: DomSanitizer
  ) {
  }

  ngOnInit(): void {
    // Ecoute du topic "video" du Websocket
    this.videoSubscription = this.videoService.getMessageObservable()
      .subscribe((videoEventMsg: Message) => this.onNewVideoEventMsg(videoEventMsg));
    // Ecoute du topic "audio" du Websocket
    this.audioSubscription = this.audioService.getMessageObservable()
      .subscribe((audioEventMsg: Message) => this.onNewAudioEventMsg(audioEventMsg));
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
    this.currentEvent = plainToClass(VideoEvent, JSON.parse(videoEventMsg.body));
    this.imgContent = this.sanitizer.bypassSecurityTrustResourceUrl(`data:image/jpeg;base64,${this.currentEvent.imageBase64}`);
    // Récupération des dimensions originales de l'image si elles n'ont pas déjà été récupérées
    if (this.imageElement && (this.imageWidth === 0 || this.imageHeight === 0)) {
      this.imageWidth = this.imageElement.nativeElement.naturalWidth;
      this.imageHeight = this.imageElement.nativeElement.naturalHeight;
    }
  }

  /**
   * Lit la frame audio reçue dans l'évènement.
   * @param audioEventMsg le message reçu dans le Websocket
   */
  private onNewAudioEventMsg(audioEventMsg: Message): void {
    const audioEvent = JSON.parse(audioEventMsg.body);
    const source = this.context.createBufferSource();
    this.context.decodeAudioData(decode(audioEvent.audioContentBase64)).then((buffer) => {
      source.buffer = buffer;
      source.connect(this.context.destination);
      source.start(this.context.currentTime);
    }).catch((error) => {
      console.log(error);
    });
  }

  /**
   * Calcule et récupère le style de la position à appliquer aux contours de la box d'un objet détecté.
   * @param detectedObject l'objet détecté
   */
  public getBoundsStyle(detectedObject: DetectedObject): string {
    return 'left: calc(' + (detectedObject.x * 100 / this.imageWidth) + '% + 5px);'
      + 'top: calc(' + (detectedObject.y * 100 / this.imageHeight) + '% + 5px);'
      + 'width: ' + (detectedObject.width * 100 / this.imageWidth) + '%;'
      + 'height: ' + (detectedObject.height * 100 / this.imageHeight) + '%;';
  }

  /**
   * Calcule et récupère le style à appliquer à la box du libellé d'un objet détecté.
   * @param detectedObject l'objet détecté
   */
  public getLabelStyle(detectedObject: DetectedObject): string {
    return 'left: calc(' + (detectedObject.x * 100 / this.imageWidth) + '% + 5px);'
      + 'top:' + ((detectedObject.y + detectedObject.height) * 100 / this.imageHeight) + '%;'
      + 'min-width: ' + (detectedObject.width * 100 / this.imageWidth) + '%;'
      + 'height: 25px;';
  }

  /**
   * Construit les coordonnées SVG des points des repères du visage.
   * @param points tableau des points 2D des repères
   */
  public getLandmarkSvgPoints(points: Point2d[]): string {
    return points.map(point => {
        return point.x + ' ' + point.y;
      }
    ).join(', ');
  }

  /**
   * Récupère la dimension de la "view box" SVG.
   */
  public getViewBox(): string {
    return '0 0 ' + this.imageWidth + ' ' + this.imageHeight;
  }
}
