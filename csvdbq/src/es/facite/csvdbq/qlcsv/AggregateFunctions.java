package es.facite.csvdbq.qlcsv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class AggregateFunctions implements Iterable<AggregateFunction>{
	private List<AggregateFunction> funcs;
	
	public AggregateFunctions() {
		funcs = new ArrayList<>();
	}
	
	public int size() {
		return funcs.size();
	}
	
	public void add(AggregateFunction af) {
		funcs.add(af);
	}
	
	public void addAll(Collection<? extends AggregateFunction> afs) {
		funcs.addAll(afs);
	}
	
	public List<AggregateFunction> get() {
		return funcs;
	}
	
	public AggregateFunction get(int index) {
		return funcs.get(index);
	}
	
	public Stream<AggregateFunction> stream() {
		return funcs.stream();
	}
	
	@Override
	public String toString() {
		var sb = new StringBuilder();	
		var eFirst = false;
		
		sb.append("AggregateFunctions [");
		sb.append(System.lineSeparator());		
		for(var e : funcs) {
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
	public Iterator<AggregateFunction> iterator() {
		return funcs.iterator();
	}
}
