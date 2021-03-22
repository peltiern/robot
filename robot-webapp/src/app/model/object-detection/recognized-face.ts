import {DetectedObject} from './detected-object';
import {Type} from 'class-transformer';
import {FaceLandmarks} from './face-landmarks';

export class RecognizedFace extends DetectedObject {
    @Type(() => FaceLandmarks)
    landmarks: FaceLandmarks;
}
