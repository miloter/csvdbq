package es.facite.csvdbq.test;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Insert;

public class C06Insert {
	public static void main(String[] args) {			
		long tStart = System.currentTimeMillis();
		
		var insert = new Insert(new CsvConfig());		
//		var count = insert.execute("insert into small2(Nombre, ID, num, activo) values"
//				+ "('miguel\\r\\nLópez\\r\\nTercero\\r\\nAlarcón', 23, 1.29, true), "
//				+ "('miguel\\r\\nLópez\\r\\nTercero\\r\\nAlarcón', 23, 1.29, true)"
//		);
		var count = insert.execute("INSERT INTO command_history(commands) VALUES ('comando 1'), ('comando 2'), ('comando 3'), ('select * from groups'), ('')");
		
		
		System.out.println(count + " filas insertadas.");
		
		System.out.println("Procesado en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
