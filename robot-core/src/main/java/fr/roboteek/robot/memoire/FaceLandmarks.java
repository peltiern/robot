package fr.roboteek.robot.memoire;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.geometry.euclidean.twod.Vector2D;

import java.util.List;

public class FaceLandmarks {

    private List<Vector2D> chin;

    private List<Vector2D> leftEyebrow;

    private List<Vector2D> rightEyebrow;

    private List<Vector2D> noseBridge;

    private List<Vector2D> noseTip;

    private List<Vector2D> leftEye;

    private List<Vector2D> rightEye;

    private List<Vector2D> topLip;

    private List<Vector2D> bottomLip;

    public List<Vector2D> getChin() {
        return chin;
    }

    public void setChin(List<Vector2D> chin) {
        this.chin = chin;
    }

    public List<Vector2D> getLeftEyebrow() {
        return leftEyebrow;
    }

    public void setLeftEyebrow(List<Vector2D> leftEyebrow) {
        this.leftEyebrow = leftEyebrow;
    }

    public List<Vector2D> getRightEyebrow() {
        return rightEyebrow;
    }

    public void setRightEyebrow(List<Vector2D> rightEyebrow) {
        this.rightEyebrow = rightEyebrow;
    }

    public List<Vector2D> getNoseBridge() {
        return noseBridge;
    }

    public void setNoseBridge(List<Vector2D> noseBridge) {
        this.noseBridge = noseBridge;
    }

    public List<Vector2D> getNoseTip() {
        return noseTip;
    }

    public void setNoseTip(List<Vector2D> noseTip) {
        this.noseTip = noseTip;
    }

    public List<Vector2D> getLeftEye() {
        return leftEye;
    }

    public void setLeftEye(List<Vector2D> leftEye) {
        this.leftEye = leftEye;
    }

    public List<Vector2D> getRightEye() {
        return rightEye;
    }

    public void setRightEye(List<Vector2D> rightEye) {
        this.rightEye = rightEye;
    }

    public List<Vector2D> getTopLip() {
        return topLip;
    }

    public void setTopLip(List<Vector2D> topLip) {
        this.topLip = topLip;
    }

    public List<Vector2D> getBottomLip() {
        return bottomLip;
    }

    public void setBottomLip(List<Vector2D> bottomLip) {
        this.bottomLip = bottomLip;
    }
}
