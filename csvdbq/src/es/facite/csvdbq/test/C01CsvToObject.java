package es.facite.csvdbq.test;

import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.core.CsvToObject;

public class C01CsvToObject {
	public static void main(String[] args) {	
		CsvRows csvStore = CsvToObject.parseFromFile("entities1.csv", '"', ',', false);
		System.out.println(csvStore);
	}
}
