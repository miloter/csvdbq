package es.facite.csvdbq.iterator;

import java.nio.charset.StandardCharsets;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvFileScanner;

public class CsvFileIterator extends CsvIterator {
	public CsvFileIterator(String tableName, CsvConfig csvConfig) {
		super(new CsvFileScanner(csvConfig.composePath(tableName), false, StandardCharsets.UTF_8), csvConfig);		
	}	
}
