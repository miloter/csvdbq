package es.facite.csvdbq.iterator;

import java.nio.charset.StandardCharsets;

import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvFileScanner;

public class CsvFileHeaders extends CsvHeaders {
	public CsvFileHeaders(String tableName, CsvConfig config) {
		super(new CsvFileScanner(config.composePath(tableName), false, StandardCharsets.UTF_8), config);		
	}	
}
