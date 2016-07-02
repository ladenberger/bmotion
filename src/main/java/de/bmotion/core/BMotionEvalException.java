package de.bmotion.core;

public class BMotionEvalException extends BMotionException {

	private static final long serialVersionUID = 1L;

	public BMotionEvalException(String msg, String formula) {
		super("Formula '" + formula + "' cannot be evaluated (" + msg + ")");
	}

}
