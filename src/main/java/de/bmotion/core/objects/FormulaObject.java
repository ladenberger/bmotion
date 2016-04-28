package de.bmotion.core.objects;

import java.util.HashMap;

public class FormulaObject {

	private String formula;

	private HashMap<String, Object> options;

	public FormulaObject() {
	}

	public FormulaObject(String formula, HashMap<String, Object> options) {
		super();
		this.formula = formula;
		this.options = options;
	}

	public FormulaObject(String formula) {
		this(formula, new HashMap<String, Object>());
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public HashMap<String, Object> getOptions() {
		return options;
	}

	public void setOptions(HashMap<String, Object> options) {
		this.options = options;
	}

}
