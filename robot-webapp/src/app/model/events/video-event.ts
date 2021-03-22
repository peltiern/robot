import {RobotEvent} from './robot-event';
import {Type} from 'class-transformer';
import {DetectedObject} from '../object-detection/detected-object';
import {RecognizedFace} from '../object-detection/recognized-face';

const VIDEO_EVENT_TYPE = 'video';

export class VideoEvent extends RobotEvent {

  imageBase64: string;
  faceFound: boolean;
  @Type(() => RecognizedFace)
  faces: RecognizedFace[];
  objectFound: boolean;
  @Type(() => DetectedObject)
  objects: DetectedObject[];

  constructor() {
    super(VIDEO_EVENT_TYPE);
  }
}

