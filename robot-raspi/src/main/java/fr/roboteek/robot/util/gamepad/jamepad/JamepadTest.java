package fr.roboteek.robot.util.gamepad.jamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public class JamepadTest {

    public static int NUM_CONTROLLERS = 4;

    public static void run() {
        ControllerManager controllers = new ControllerManager(NUM_CONTROLLERS);
        controllers.initSDLGamepad();

        int i = 0;
        while (true) {
            i++;
            ControllerState currState = controllers.getState(0);

            if (!currState.isConnected || currState.back) {
                break;
            }

            StringBuffer sb = new StringBuffer();

            if (currState.a) {
                sb.append(i + "-CROSS-");
            }
            if (currState.b) {
                sb.append(i + "-ROUND-");
            }
            if (currState.x) {
                sb.append(i + "-SQUARE-");
            }
            if (currState.y) {
                sb.append(i + "-TRIANGLE-");
            }
            if (currState.lb) {
                sb.append(i + "-LB-");
            }
            if (currState.rb) {
                sb.append(i + "-RB-");
            }
            if (currState.start) {
                sb.append(i + "-start-");
            }
            if (currState.back) {
                sb.append(i + "-back-");
            }
            if (currState.guide) {
                sb.append(i + "-guide-");
            }
            if (currState.dpadUp) {
                sb.append(i + "-dpadUp-");
            }
            if (currState.dpadDown) {
                sb.append(i + "-dpadDown-");
            }
            if (currState.dpadLeft) {
                sb.append(i + "-dpadLeft-");
            }
            if (currState.dpadRight) {
                sb.append(i + "-dpadRight-");
            }

            if (sb.length() > 0) {
                System.out.println(sb);
            }

            System.out.println(currState.leftStickAngle + " - " + currState.leftStickMagnitude + " - " + currState.leftStickClick + " - " + currState.leftTrigger + " - " + currState.rightTrigger);

//            System.out.println(currState.leftStickX + " - " + currState.leftStickY);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        controllers.quitSDLGamepad();
    }

    public static void main(String[] args) {

        run();
    }
}
