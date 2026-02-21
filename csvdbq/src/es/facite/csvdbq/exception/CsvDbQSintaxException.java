package es.facite.csvdbq.exception;

import es.facite.text.Scanner;

public class CsvDbQSintaxException extends CsvDbQException {	
	private static final long serialVersionUID = -1260341253818422989L;

	public CsvDbQSintaxException(String tokenExpected, Scanner scan) {
		super("Se esperaba '" + tokenExpected
				+ "' pero se obtuvo '" + scan.lex() + "', en la línea "
				+ scan.tokenLin() + ", columna " + scan.tokenCol());
	}
	
	public CsvDbQSintaxException(String msg) {
		super(msg);
	}
}
