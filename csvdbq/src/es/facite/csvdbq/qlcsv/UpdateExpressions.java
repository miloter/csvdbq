package es.facite.csvdbq.qlcsv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class UpdateExpressions implements Iterable<UpdateExpression>{
	private List<UpdateExpression> expressions;
	
	public UpdateExpressions() {
		expressions = new ArrayList<>();
	}
	
	public int size() {
		return expressions.size();
	}
	
	public void add(UpdateExpression se) {
		expressions.add(se);
	}
	
	public List<UpdateExpression> get() {
		return expressions;
	}
	
	public UpdateExpression get(int index) {
		return expressions.get(index);
	}
	
	public Stream<UpdateExpression> stream() {
		return expressions.stream();
	}
	
	@Override
	public String toString() {
		var sb = new StringBuilder();	
		var eFirst = false;
		
		sb.append("UpdateExpressions [");
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
	public Iterator<UpdateExpression> iterator() {
		return expressions.iterator();
	}
}
