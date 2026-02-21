package es.facite.csvdbq.qlcsv;

public class LimitExpression {
	private int offset;
	private int count;
	
	public LimitExpression(int offset, int count) {		
		this.offset = offset;
		this.count = count;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "LimiExpression [offset=" + offset + ", count=" + count + "]";
	}	
}
