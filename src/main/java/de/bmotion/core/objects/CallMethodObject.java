package de.bmotion.core.objects;

public class CallMethodObject {

	private String sessionId;
	
	private String name;
	
	private Object[] arguments;
	
	public CallMethodObject() {
	}
	
	public CallMethodObject(String sessionId, String name, Object[] arguments) {
		super();
		this.sessionId = sessionId;
		this.name = name;
		this.arguments = arguments;		
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

	public Object[] getArguments() {
		return arguments;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

}
