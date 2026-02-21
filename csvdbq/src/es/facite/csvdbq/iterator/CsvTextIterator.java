package es.facite.csvdbq.iterator;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvTextScanner;

public class CsvTextIterator extends CsvIterator {
	public CsvTextIterator(String csvText, CsvConfig csvConfig) {
		super(new CsvTextScanner(csvText, false), csvConfig);		
	}	
}
