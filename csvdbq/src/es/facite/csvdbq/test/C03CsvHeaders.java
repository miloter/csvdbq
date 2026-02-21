package es.facite.csvdbq.test;

import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.qlcsv.CsvConfig;

@SuppressWarnings("unused")
public class C03CsvHeaders {
	static final String DB_DIR = "./";
	static final char QUOTES = '"';
	static final char SEP = ';';
	static final boolean DECIMAL_SEPARATOR_POINT = false;
	
	
	public static void main(String[] args) {
		long tStart = System.currentTimeMillis();
		
		try (var csvHeaders = new CsvFileHeaders("entities1", new CsvConfig('"', ',', false))) {
			System.out.println(csvHeaders);
			// Se pueden leer el resto de filas
			var it = csvHeaders.getIterator();
			while (it.hasNext()) {
				var row = it.next();
				System.out.println(row);
			}
		}
		
		System.out.println("Procesado en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
