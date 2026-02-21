package es.facite.csvdbq.qlcsv;

public class SelectExpression {
	private String expression;
	private String label;
	// Indica si contiene funciones de agregado
	private boolean aggFunctions;
	
	public SelectExpression(String expression) {
		this(expression, expression);
	}
	
	public SelectExpression(String expression, String label) {
		this.expression = expression;
		this.label = label;		
	}
	
	public String getExpression() {
		return expression;
	}

	public String getLabel() {
		return label;
	}		

	public boolean isAggFunctions() {
		return aggFunctions;
	}

	public void setAggFunctions(boolean aggFunctions) {
		this.aggFunctions = aggFunctions;
	}

	@Override
	public String toString() {
		return "SelectExpression [expression=" + expression + ", label=" + label +
				", aggFunctions=" + aggFunctions + "]";
	}			
}
