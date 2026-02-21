package es.facite.csvdbq.test;

import es.facite.csvdbq.core.CsvDecimalFormat;

public class Test {
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		var df = new CsvDecimalFormat(false);
		
		System.out.println(df.grouped(1234567.89));
		System.out.println(df.grouped(1234567.89));
		System.out.println(df.grouped(12345.07));
		System.out.println(df.grouped(12345678901234567890123456789.0));
	}
}
