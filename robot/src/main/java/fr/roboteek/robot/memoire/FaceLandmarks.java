package fr.roboteek.robot.memoire;

import com.google.gson.annotations.SerializedName;
import org.openimaj.math.geometry.point.Point2dImpl;

import java.util.List;

public class FaceLandmarks {

    @SerializedName("chin")
    private List<Point2dImpl> chin;

    @SerializedName("left_eyebrow")
    private List<Point2dImpl> leftEyebrow;

    @SerializedName("right_eyebrow")
    private List<Point2dImpl> rightEyebrow;

    @SerializedName("nose_bridge")
    private List<Point2dImpl> noseBridge;

    @SerializedName("nose_tip")
    private List<Point2dImpl> noseTip;

    @SerializedName("left_eye")
    private List<Point2dImpl> leftEye;

    @SerializedName("right_eye")
    private List<Point2dImpl> rightEye;

    @SerializedName("top_lip")
    private List<Point2dImpl> topLip;

    @SerializedName("bottom_lip")
    private List<Point2dImpl> bottomLip;

    public List<Point2dImpl> getChin() {
        return chin;
    }

    public void setChin(List<Point2dImpl> chin) {
        this.chin = chin;
    }

    public List<Point2dImpl> getLeftEyebrow() {
        return leftEyebrow;
    }

    public void setLeftEyebrow(List<Point2dImpl> leftEyebrow) {
        this.leftEyebrow = leftEyebrow;
    }

    public List<Point2dImpl> getRightEyebrow() {
        return rightEyebrow;
    }

    public void setRightEyebrow(List<Point2dImpl> rightEyebrow) {
        this.rightEyebrow = rightEyebrow;
    }

    public List<Point2dImpl> getNoseBridge() {
        return noseBridge;
    }

    public void setNoseBridge(List<Point2dImpl> noseBridge) {
        this.noseBridge = noseBridge;
    }

    public List<Point2dImpl> getNoseTip() {
        return noseTip;
    }

    public void setNoseTip(List<Point2dImpl> noseTip) {
        this.noseTip = noseTip;
    }

    public List<Point2dImpl> getLeftEye() {
        return leftEye;
    }

    public void setLeftEye(List<Point2dImpl> leftEye) {
        this.leftEye = leftEye;
    }

    public List<Point2dImpl> getRightEye() {
        return rightEye;
    }

    public void setRightEye(List<Point2dImpl> rightEye) {
        this.rightEye = rightEye;
    }

    public List<Point2dImpl> getTopLip() {
        return topLip;
    }

    public void setTopLip(List<Point2dImpl> topLip) {
        this.topLip = topLip;
    }

    public List<Point2dImpl> getBottomLip() {
        return bottomLip;
    }

    public void setBottomLip(List<Point2dImpl> bottomLip) {
        this.bottomLip = bottomLip;
    }
}
