package es.facite.csvdbq.qlcsv;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;

public class ResultSelect {
	private CsvRow headers;
	private CsvRows rows;
	
	public ResultSelect(CsvRow headers, CsvRows rows) {	
		this.headers = headers;
		this.rows = rows;
	}
	
	public int size() {
		return rows.size();
	}
	
	public CsvRow getHeaders() {
		return headers;
	}

	public CsvRows getRows() {
		return rows;
	}
	
	private void printRowSeparator(StringBuilder sb, int[] widths) {
		for (int i = 0; i < widths.length; i++) {
			sb.append('+');
			sb.append('-');
			sb.repeat('-', widths[i]);
			sb.append('-');			
		}
		sb.append('+');		
		sb.append(System.lineSeparator());
	}
	
	private void printValue(StringBuilder sb, int width, String value) {
		sb.append('|');
		sb.append(' ');
		if (value.length() > width) {
			value = value.substring(0, width);
		}
		sb.append(value);	
		sb.repeat(' ', width - value.length());			
		sb.append(' ');
	}
	
	@Override
	public String toString() {
		final int MAX_ROWS = 1024;
		final int MAX_WIDTH = 128;
		final int[] widths = new int[headers.size()];		
		StringBuilder sb = new StringBuilder();
		
		// Calcula el ancho de cada columna comenzando por las cabeceras
		for (int i = 0; i < headers.size(); i++) {			
			widths[i] = Math.min(headers.toString(i).length(), MAX_WIDTH);
		}
		
		// Calcula el ancho procesando un máximo de las MAX_ROWS primeras filas
		for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
			var row = rows.get(i);
			
			for (int j = 0; j < row.size(); j++) {			
				widths[j] = Math.min(
						Math.max(widths[j], row.toString(j).length()),
						MAX_WIDTH);
			}	
		}

		// Imprime las cabeceras
		printRowSeparator(sb, widths);				
		for (int i = 0; i < headers.size(); i++) {
			printValue(sb, widths[i], headers.toString(i));			
		}
		sb.append('|');
		sb.append(System.lineSeparator());
		printRowSeparator(sb, widths);
		
		// Imprime las filas
		for (int i = 0; i < Math.min(MAX_ROWS, rows.size()); i++) {
			var row = rows.get(i);
			
			for (int j = 0; j < row.size(); j++) {
				printValue(sb, widths[j], row.toString(j));
			}
			sb.append('|');
			sb.append(System.lineSeparator());	
		}
		printRowSeparator(sb, widths);
		
		return sb.toString();
	}
}
