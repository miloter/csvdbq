package es.facite.csvdbq.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import es.facite.csvdbq.qlcsv.Evaluator;

public class Util {
	public static final boolean isInteger(double num) {
		return Double.isFinite(num) && num == Math.floor(num);
	}
	
	public static final boolean isInteger(double num, int min, int max) {
		return isInteger(num) && num >= min && num <= max;
	}		
	
	public static boolean evalWhere(Evaluator evaluator, String where) {
		if (where == null) return true;	
		
		return isTrue(evaluator.eval(where));		
	}
	
	public static boolean isTrue(Object o) {		
		if (o instanceof Boolean bln) return bln;
		
		if (o == null) return false;
		
		if (o instanceof Double dbl) {
			return dbl.compareTo(0.0) != 0;
		}
		
		return true;
	}
	
	/**
	 * Crea un fichero temporal y devuelve un objeto Path que lo referencia.
	 * @return
	 */
	public static Path createTempFile() {
		try {
			return Files.createTempFile(null, null);
		} catch (IOException e) {			
			throw new RuntimeException(e);
		}	
	}
}
