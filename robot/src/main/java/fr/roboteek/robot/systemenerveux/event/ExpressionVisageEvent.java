package fr.roboteek.robot.systemenerveux.event;

/**
 * Evènement pour bouger la tête.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ExpressionVisageEvent extends RobotEvent {
	
	public static final String EVENT_TYPE = "ExpressionVisage";

    private String expression;

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return "ExpressionVisageEvent [expression=" + expression + "]";
	}
}
