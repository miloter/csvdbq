package es.facite.csvdbq.qlcsv;

public class AggregateExpression {	
	private String expression; // Por ejemplo: sum(salary1 + salary2 * 1.23)
	private String innerExpression; // salary1 + salary2 * 1.23
	
	public AggregateExpression(String expression, String innerExpression) {
		this.expression = expression;		
		this.innerExpression = innerExpression;
	}	

	public String getExpression() {
		return expression;
	}

	public String getInnerExpression() {
		return innerExpression;
	}		

	@Override
	public String toString() {
		return "AggregateExpression [expression=" + expression +
				", innerExpression=" + innerExpression + "]";
	}			
}
