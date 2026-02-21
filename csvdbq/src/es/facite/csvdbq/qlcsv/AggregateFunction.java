package es.facite.csvdbq.qlcsv;

public class AggregateFunction {
	private int aggFuncId;
	private AggregateExpression aggregateExpression;
	private Object value; // Valor de la función de agregado	
	// Cuenta el número de elementos sean o no nulos
	private int count;
	// Cuenta el número de elementos no nulos
	private int countNotNull;
	
	public AggregateFunction(int aggFuncId, AggregateExpression aggregateExpression) {
		this.aggFuncId = aggFuncId;
		this.aggregateExpression = aggregateExpression;
		value = null;
		count = 0;
		countNotNull = 0;
	}
	
	public int getAggFuncId() {
		return aggFuncId;
	}

	public AggregateExpression getAggregateExpression() {
		return aggregateExpression;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	public int getCount() {
		return count;
	}
	
	public void incCount() {
		count++;
	}

	public int getCountNotNull() {
		return countNotNull;
	}

	public void incCountNotNull() {
		countNotNull++;
	}

	@Override
	public String toString() {
		return "AggregateFunction [aggFuncId=" + aggFuncId +
				", aggregateExpression=" + aggregateExpression +
				", value=" + value +
				", count=" + count +
				", countAvgNotNull=" + countNotNull + "]";
	}			
}
