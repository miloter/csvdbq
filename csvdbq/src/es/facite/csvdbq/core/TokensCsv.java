package es.facite.csvdbq.core;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.text.CsvScanner;

public class TokensCsv {
	/**
	 * Caracteres de entrecomillado permitidos.
	 */
	public static final char[] CSV_QUOTES_VALID = { '"', '\'' };
	/**
	 * Caracteres separadores de campo permitidos.
	 */
	public static final char[] CSV_SEP_VALID = { ';', ',', '\t' };
	/**
	 * Caracter de entrecomillado por defecto.
	 */
	public static final char DEFAULT_QUOTES = '"';
	/**
	 * Caracter separador por defecto;
	 */
	public static final char DEFAULT_SEP = ';';
	/**
	 * Caracter de nueva línea en ficheros JSON.
	 */
	public static final String NL = "\r\n";
	/**
	 * Indica si se usa o no por defecto el punto decimal.
	 */
	public static final boolean DEFAULT_DS_POINT = new DecimalFormatSymbols(Locale.getDefault())
			.getDecimalSeparator() == '.';	
	// Tokens de una secuencia CSV
	public static final int SEP = 0;	
	public static final int TRUE = 1;
	public static final int FALSE = 2;
	public static final int NULL = 3;
	public static final int MINUS = 4;
	
	public static void initialize(CsvScanner scan, char quotes, char sep) {
		initialize(scan, quotes, sep, DEFAULT_DS_POINT);
	}
	
	public static void initialize(CsvScanner scan, char quotes, char sep, boolean decimalSeparatorPoint) {
		// Configuración general del Scanner
		scan.setOperatorString(String.valueOf(quotes));		
		scan.setDecimalSeparatorPoint(decimalSeparatorPoint);

		// Operadores
		scan.operatorAdd(SEP, String.valueOf(sep));
		scan.operatorAdd(MINUS, "-");
		
		// Palabras clave
		scan.keywordAdd(TRUE, "true");
		scan.keywordAdd(FALSE, "false");
		scan.keywordAdd(NULL, "null");
	}
	
	public static void checkQuotesAndSep(char quotes, char sep) {
		checkQuotes(quotes);
		checkSep(sep);
	}
	
	public static void checkQuotes(char quotes) {
		if (!contains(CSV_QUOTES_VALID, quotes)) {
			throw new CsvDbQSintaxException("Caracter de entrecomillado inválido, solo uno de: " +
					Arrays.toString(CSV_QUOTES_VALID));
		}
	}
	
	public static void checkSep(char sep) {
		if (!contains(CSV_SEP_VALID, sep)) {
			throw new CsvDbQSintaxException("Caracter separador no válido, solo uno de: " +
					Arrays.toString(CSV_SEP_VALID));
		}
	}
	
	/**
	 * Determina si un array de caracteres, contiene un carácter.
	 * 
	 * @param arr
	 * @param ch
	 * @return
	 */
	private static boolean contains(char[] arr, char ch) {
		for (char c : arr) {
			if (c == ch) {
				return true;
			}
		}

		return false;
	}
}
