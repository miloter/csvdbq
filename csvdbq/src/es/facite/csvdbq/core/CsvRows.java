package es.facite.csvdbq.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CsvRows implements Iterable<CsvRow> {
	private final List<CsvRow> rows;

	public CsvRows() {
		rows = new ArrayList<>();
	}
	
	public CsvRows(CsvRows rows, int fromIndex, int toIndex) {
		this.rows = rows.getRows().subList(fromIndex, toIndex);
	}

	public List<CsvRow> getRows() {
		return rows;
	}

	public void add(CsvRow row) {
		rows.add(row);
	}
	
	public void add(int index, CsvRow row) {
		rows.add(index, row);
	}

	public int size() {
		return rows.size();
	}	

	public void sort(int colIndex, boolean asc) {
		mergeSort(colIndex, asc ? 1 : -1, this, size());
	}
	
	public CsvRow get(int index) {
		return rows.get(index);
	}
	
	private void set(int index, CsvRow row) {
		rows.set(index, row);
	}
	
	private void mergeSort(int colIndex, int order, CsvRows a, int n) {
		if (n < 2) {
			return;
		}
		int mid = n / 2;
		CsvRows l = new CsvRows();
		CsvRows r = new CsvRows();
		
		for (int i = 0; i < mid; i++) {
			l.add(a.get(i));			
		}
		for (int i = mid; i < n; i++) {
			r.add(a.get(i));
		}
		mergeSort(colIndex, order, l, mid);
		mergeSort(colIndex, order, r, n - mid);
		
		merge(colIndex, order, a, l, r, mid, n - mid);
	}
	
	private void merge(int colIndex, int order, CsvRows a, CsvRows l, CsvRows r, int left, int right) {
		int i = 0, j = 0, k = 0;
		while (i < left && j < right) {
			if (compare(l.get(i).get(colIndex), r.get(j).get(colIndex)) * order <= 0) {
				a.set(k++, l.get(i++));
			} else {
				a.set(k++, r.get(j++));
			}
		}
		while (i < left) {
			a.set(k++, l.get(i++));
		}
		while (j < right) {
			a.set(k++, r.get(j++));
		}
	}
	
	private int compare(Object o1, Object o2) {
		if (o1 == o2)
			return 0;
		if (o1 == null)
			return -1;
		if (o2 == null)
			return 1;

		if (o1 instanceof Double dbl1) {
			if (o2 instanceof Double dbl2) {
				return dbl1.compareTo(dbl2);
			} else if (o2 instanceof Boolean) {
				return 1;
			}
			// o2 es una instancia de String
			return -1;
		} else if (o1 instanceof String str1) {
			if (o2 instanceof String str2) {
				return str1.compareToIgnoreCase(str2);
			}
			// o2 es una instancia de Boolean o Double
			return 1;
		}
		// o1 es una instancia de Boolean
		if (o2 instanceof Boolean bln2) {
			return ((Boolean) o1).compareTo(bln2);
		}
		// o2 es una instancia de Double o String
		return -1;
	}

	@Override
	public String toString() {
		var sbRows = new StringBuilder();

		for (var row : rows) {
			if (sbRows.length() > 0) {
				sbRows.append(TokensCsv.NL);
			}
			sbRows.append(row);
		}

		return sbRows.toString();
	}

	@Override
	public Iterator<CsvRow> iterator() {
		return rows.iterator();
	}
}
