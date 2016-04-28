package de.bmotion.core.objects;

import java.util.Map;

public class ExecuteEventObject {

	private String sessionId;

	private String name;

	private Map<String, String> options;

	public ExecuteEventObject() {
	}

	public ExecuteEventObject(String sessionId, String name, Map<String, String> options) {
		super();
		this.sessionId = sessionId;
		this.name = name;
		this.options = options;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

}
