package de.bmotion.core.objects;

public class FormulaReturnObject {

	private String formula;
	private Object result;
	private String error;
	
	public FormulaReturnObject() {
	}

	public FormulaReturnObject(String formula) {
		super();
		this.formula = formula;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
