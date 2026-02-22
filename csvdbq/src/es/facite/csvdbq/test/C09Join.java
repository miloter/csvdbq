package es.facite.csvdbq.test;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Join;

public class C09Join {
	static final String DB_DIR = "./";
	static final char QUOTES = '"';
	static final char SEP = ';';
	static final boolean DECIMAL_SEPARATOR_POINT = false;
	
	public static void main(String[] args) {			
		long tStart = System.currentTimeMillis();
		
		var dbManager = new CsvConfig(DB_DIR, QUOTES, SEP, DECIMAL_SEPARATOR_POINT);
		var join = new Join(dbManager);
		var rows = join.execute("join(select * from dir_empleados; "
				+ "select * from dir_empleados; id)");
		
		System.out.println(rows);		
		System.out.println(rows.size() + " filas devueltas en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
