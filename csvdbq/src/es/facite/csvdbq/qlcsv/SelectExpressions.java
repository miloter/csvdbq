package es.facite.csvdbq.qlcsv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SelectExpressions implements Iterable<SelectExpression>{
	private List<SelectExpression> expressions;
	
	public SelectExpressions() {
		expressions = new ArrayList<>();
	}
	
	public int size() {
		return expressions.size();
	}
	
	public void add(SelectExpression se) {
		expressions.add(se);
	}
	
	public List<SelectExpression> get() {
		return expressions;
	}
	
	public SelectExpression get(int index) {
		return expressions.get(index);
	}
	
	public Stream<SelectExpression> stream() {
		return expressions.stream();
	}
	
	@Override
	public String toString() {
		var sb = new StringBuilder();	
		var eFirst = false;
		
		sb.append("SelectExpressions [");
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
	public Iterator<SelectExpression> iterator() {
		return expressions.iterator();
	}
}
