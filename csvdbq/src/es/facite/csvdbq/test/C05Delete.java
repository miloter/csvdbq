package es.facite.csvdbq.test;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Delete;

public class C05Delete {
	public static void main(String[] args) {			
		long tStart = System.currentTimeMillis();
		
		var delete = new Delete(new CsvConfig(".", '"', ',', false));
		var count = delete.execute("delete from entities2 where Type = 'Regional entities'");
		System.out.println(count + " filas eliminadas.");
		
		System.out.println("Procesado en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
