package es.facite.csvdbq.test;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Select;

public class C04Select {
	static final String DB_DIR = "./";
	static final char QUOTES = '"';
	static final char SEP = ';';
	static final boolean DECIMAL_SEPARATOR_POINT = false;
	
	public static void main(String[] args) {			
		long tStart = System.currentTimeMillis();
		
		var dbManager = new CsvConfig(DB_DIR, QUOTES, SEP, DECIMAL_SEPARATOR_POINT);
		var select = new Select(dbManager);
		var rows = select.execute("select ape_nom1, count(ape_nom1) as total from small "
				+ "group by ape_nom1 "
				+ "order by total desc");
//		var rows = select.execute("select admin, num from small2 "
//				+ "where admin between 300 and 3023 "
//				+ "order by admin desc");
		System.out.println(rows);
		
		System.out.println(rows.size() + " filas devueltas en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
