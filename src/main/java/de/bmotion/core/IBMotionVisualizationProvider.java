package de.bmotion.core;

import java.util.Map;

public interface IBMotionVisualizationProvider {

	public BMotion get(String sessionId, String model, Map<String, String> options);

}
