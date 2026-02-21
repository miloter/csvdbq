package es.facite.csvdbq.qlcsv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class OrderByExpressions implements Iterable<OrderByExpression>{
	private List<OrderByExpression> expressions;
	
	public OrderByExpressions() {
		expressions = new ArrayList<>();
	}
	
	public int size() {
		return expressions.size();
	}
	
	public void add(OrderByExpression se) {
		expressions.add(se);
	}
	
	public List<OrderByExpression> get() {
		return expressions;
	}
	
	public OrderByExpression get(int index) {
		return expressions.get(index);
	}
	
	public Stream<OrderByExpression> stream() {
		return expressions.stream();
	}
	
	@Override
	public String toString() {
		var sb = new StringBuilder();	
		var eFirst = false;
		
		sb.append("OrderByExpressions [");
		sb.append(System.lineSeparator());		
		for(var e : expressions) {
			if (eFirst) {
				sb.append(',');
				sb.append(System.lineSeparator());
			} else {
				eFirst = true;
			}
			sb.append('\t');
			sb.append(e);
			
		}
		sb.append(System.lineSeparator());
		sb.append(']');
		
		return sb.toString();
	}

	@Override
	public Iterator<OrderByExpression> iterator() {
		return expressions.iterator();
	}
}
