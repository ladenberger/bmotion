package de.bmotion.core.objects;

import java.util.Map;

public class ObserverFormulaListObject {

	private String sessionId;

	private Map<String, FormulaListObject> formulas;

	public ObserverFormulaListObject() {
	}

	public ObserverFormulaListObject(String sessionId, Map<String, FormulaListObject> formulas) {
		super();
		this.sessionId = sessionId;
		this.formulas = formulas;
	}

	public Map<String, FormulaListObject> getFormulas() {
		return formulas;
	}

	public void setFormulas(Map<String, FormulaListObject> formulas) {
		this.formulas = formulas;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
