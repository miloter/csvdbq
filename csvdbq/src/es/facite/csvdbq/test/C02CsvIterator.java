package es.facite.csvdbq.test;

import es.facite.csvdbq.iterator.CsvFileIterator;
import es.facite.csvdbq.qlcsv.CsvConfig;

@SuppressWarnings("unused")
public class C02CsvIterator {
	static final String DB_DIR = "./";
	static final char QUOTES = '"';
	static final char SEP = ';';
	static final boolean DECIMAL_SEPARATOR_POINT = false;
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		long tStart = System.currentTimeMillis();	
		var dbManager = new CsvConfig(DB_DIR, QUOTES, SEP, DECIMAL_SEPARATOR_POINT);
		
		for (var row : new CsvFileIterator("large", dbManager)) {		
			//System.out.println(row);						
		}
		System.out.println("CSV grande leído en " + (System.currentTimeMillis() - tStart) + " ms.");

		tStart = System.currentTimeMillis();		
		for (var row : new CsvFileIterator("medium", dbManager)) {		
			//System.out.println(row);						
		}
		System.out.println("CSV mediano leído en " + (System.currentTimeMillis() - tStart) + " ms.");
		
		tStart = System.currentTimeMillis();		
		for (var row : new CsvFileIterator("small", dbManager)) {		
			//System.out.println(row);						
		}
		System.out.println("CSV pequeño leído en " + (System.currentTimeMillis() - tStart) + " ms.");
		
		tStart = System.currentTimeMillis();		
		for (var row : new CsvFileIterator("entities1", new CsvConfig('"', ',', false))) {		
			//System.out.println(row);						
		}
		System.out.println("CSV entities1 leído en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
