package de.bmotion.core.objects;

import java.util.Map;

public class ExecuteEventObject {

	private String sessionId;

	private Map<String, String> options;

	public ExecuteEventObject() {
	}

	public ExecuteEventObject(String sessionId, String name, Map<String, String> options) {
		super();
		this.sessionId = sessionId;
		this.options = options;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

}
