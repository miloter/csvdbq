package es.facite.csvdbq.iterator;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvTextScanner;

public class CsvTextHeaders extends CsvHeaders {
	public CsvTextHeaders(String text, CsvConfig config) {
		super(new CsvTextScanner(text, false), config);		
	}	
}
