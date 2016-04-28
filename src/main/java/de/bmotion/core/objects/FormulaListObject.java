package de.bmotion.core.objects;

import java.util.List;

public class FormulaListObject {

	private List<FormulaObject> formulas;

	public FormulaListObject() {
	}

	public FormulaListObject(List<FormulaObject> formulas) {
		super();
		this.formulas = formulas;
	}

	public List<FormulaObject> getFormulas() {
		return formulas;
	}

	public void setFormulas(List<FormulaObject> formulas) {
		this.formulas = formulas;
	}

}
