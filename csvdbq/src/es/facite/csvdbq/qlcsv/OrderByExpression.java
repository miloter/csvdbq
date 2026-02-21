package es.facite.csvdbq.qlcsv;

public class OrderByExpression {
	private int index;
	private boolean asc;
	
	public OrderByExpression(int index, boolean asc) {	
		this.index = index;
		this.asc = asc;
	}

	public int getIndex() {
		return index;
	}

	public boolean isAsc() {
		return asc;
	}

	@Override
	public String toString() {
		return "OrderByExpression [index=" + index + ", asc=" + asc + "]";
	}	
}
