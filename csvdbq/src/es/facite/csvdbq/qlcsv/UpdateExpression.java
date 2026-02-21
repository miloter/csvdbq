package es.facite.csvdbq.qlcsv;

public class UpdateExpression {
	private int index;
	private String expression;
	
	public UpdateExpression(int index, String expression) {	
		this.index = index;
		this.expression = expression;
	}

	public int getIndex() {
		return index;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public String toString() {
		return "UpdateExpression [index=" + index + ", expression=" + expression + "]";
	}	
}
