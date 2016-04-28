package de.bmotion.core.objects;

import java.util.Map;

public class SessionObject {
	
	private String sessionId;
	
	private Map<String, String> options;

	public SessionObject() {
	}
	
	public SessionObject(String sessionId, Map<String, String> options) {
		this.sessionId = sessionId;
		this.options = options;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

}
