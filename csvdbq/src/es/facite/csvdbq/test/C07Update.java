package es.facite.csvdbq.test;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Update;

public class C07Update {
	static final String DB_DIR = "D:\\google-drive\\desarrollo\\java\\projects\\facite-api\\csvdbq";
	static final char QUOTES = '"';
	static final char SEP = ';';
	static final boolean DECIMAL_SEPARATOR_POINT = false;
	
	public static void main(String[] args) {			
		long tStart = System.currentTimeMillis();
		
		var dbManager = new CsvConfig(DB_DIR, QUOTES, SEP, DECIMAL_SEPARATOR_POINT);
		var update = new Update(dbManager);
		var count = update.execute("update small2 set num = num + 1 "
				+ "where admin % 2 != 0");
		System.out.println(count + " filas actualizadas.");
		
		System.out.println("Procesado en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
