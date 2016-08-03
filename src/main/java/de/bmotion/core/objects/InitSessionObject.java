package de.bmotion.core.objects;

import java.util.Map;

public class InitSessionObject {

	private String sessionId;
	private String manifestFilePath;
	private String modelPath;
	private String groovyPath;
	private Map<String, String> options;

	public InitSessionObject() {
	}

	public InitSessionObject(String sessionId, String manifestFilePath, String modelPath, String groovyPath, Map<String, String> options) {
		super();
		this.sessionId = sessionId;
		this.manifestFilePath = manifestFilePath;
		this.modelPath = modelPath;
		this.groovyPath = groovyPath;
		this.options = options;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getModelPath() {
		return modelPath;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public String getManifestFilePath() {
		return manifestFilePath;
	}

	public void setManifestFilePath(String manifestFilePath) {
		this.manifestFilePath = manifestFilePath;
	}

	public String getGroovyPath() {
		return groovyPath;
	}

	public void setGroovyPath(String groovyPath) {
		this.groovyPath = groovyPath;
	}

}
