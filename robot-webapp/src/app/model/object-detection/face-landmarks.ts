import {Type} from 'class-transformer';
import {Point2d} from './point2d';

export class FaceLandmarks {
    @Type(() => Point2d)
    chin: Point2d[];
    @Type(() => Point2d)
    leftEyebrow: Point2d[];
    @Type(() => Point2d)
    rightEyebrow: Point2d[];
    @Type(() => Point2d)
    noseBridge: Point2d[];
    @Type(() => Point2d)
    noseTip: Point2d[];
    @Type(() => Point2d)
    leftEye: Point2d[];
    @Type(() => Point2d)
    rightEye: Point2d[];
    @Type(() => Point2d)
    topLip: Point2d[];
    @Type(() => Point2d)
    bottomLip: Point2d[];
}
