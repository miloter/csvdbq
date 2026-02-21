package es.facite.csvdbq.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvFileScanner;
import es.facite.text.CsvScanner;
import es.facite.text.CsvTextScanner;
import es.facite.text.Scanner;

public class CsvToObject {
	private CsvScanner scan;
	private int token;

	private CsvToObject() {
	}

	/**
	 * Converte una cadena CSV a un objeto Java apto para JSON.
	 * 
	 * @param csv Cadena en formato CSV. Como carácter de entrecomillado de campos
	 *            (cuando se requiera) se usará {@link JsonObject#DEFAULT_QUOTES} y
	 *            como carácter separador de campos se usará
	 *            {@link JsonObject#DEFAULT_SEP}
	 * @return
	 */
	public static CsvRows parse(String csv) {
		return parse(csv, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP);
	}

	/**
	 * Converte una cadena CSV a un objeto Java apto para JSON.
	 * 
	 * @param csv    Cadena en formato CSV.
	 * @param quotes Caracter de entrecomillado para campos que lo requieran, puede
	 *               ser uno de: '\"', '\''.
	 * @para sep Separador de campos, puede ser uno de: ';', ',', '\t', '|'.
	 * @return
	 */
	public static CsvRows parse(String csv, char quotes, char sep) {
		return parse(csv, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP, TokensCsv.DEFAULT_DS_POINT);
	}

	/**
	 * Converte una cadena CSV a un objeto Java apto para JSON.
	 * 
	 * @param csv    Cadena en formato CSV.
	 * @param quotes Caracter de entrecomillado para campos que lo requieran, puede
	 *               ser uno de: '\"', '\''.
	 * @para sep Separador de campos, puede ser uno de: ';', ',', '\t', '|'.
	 * @para decimalSeparatorPoint Si es <code>true</code> Los números con separador
	 *       decimal usarán el punto (.), en otro caso la coma (,).
	 * @return
	 */
	public static CsvRows parse(String csv, char quotes, char sep, boolean decimalSeparatorPoint) {
		TokensCsv.checkQuotesAndSep(quotes, sep);
		CsvToObject ctj = new CsvToObject();

		ctj.scan = new CsvTextScanner(csv);
		TokensCsv.initialize(ctj.scan, quotes, sep, decimalSeparatorPoint);

		return ctj._parse(quotes, sep, decimalSeparatorPoint);
	}

	/**
	 * Converte una cadena CSV a un objeto Java obtenida desde un fichero.
	 * 
	 * @param csv     Cadena en formato CSV.
	 * @param pathCsv Ruta del archivo que contiene el texto CSV. Como carácter de
	 *                entrecomillado de campos (cuando se requiera) se usará
	 *                {@link JsonObject#DEFAULT_QUOTES} y como carácter separador de
	 *                campos se usará {@link JsonObject#DEFAULT_SEP}
	 * @return
	 */
	public static CsvRows parseFromFile(String pathCsv) {
		return parseFromFile(pathCsv, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP);
	}

	/**
	 * Converte una cadena CSV a un objeto Java obtenida desde un fichero.
	 * 
	 * @param pathCsv Ruta del archivo que contiene el texto CSV.
	 * @param quotes  Caracter de entrecomillado para campos que lo requieran, puede
	 *                ser uno de: '\"', '\''.
	 * @para sep Separador de campos, puede ser uno de: ';', ',', '\t', '|'.
	 * @return
	 */
	public static CsvRows parseFromFile(String pathCsv, char quotes, char sep) {
		return parseFromFile(pathCsv, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP,
				TokensCsv.DEFAULT_DS_POINT);
	}

	/**
	 * Converte una cadena CSV a un objeto Java obtenida desde un fichero.
	 * 
	 * @param pathCsv Ruta del archivo que contiene el texto CSV.
	 * @param quotes  Caracter de entrecomillado para campos que lo requieran, puede
	 *                ser uno de: '\"', '\''.
	 * @para sep Separador de campos, puede ser uno de: ';', ',', '\t', '|'.
	 * @para decimalSeparatorPoint Si es <code>true</code> Los números con separador
	 *       decimal usarán el punto (.), en otro caso la coma (,).
	 * @return
	 */
	public static CsvRows parseFromFile(String pathCsv, char quotes, char sep, boolean decimalSeparatorPoint) {
		TokensCsv.checkQuotesAndSep(quotes, sep);
		CsvToObject ctj = new CsvToObject();

		ctj.scan = new CsvFileScanner(pathCsv, false, StandardCharsets.UTF_8);
		TokensCsv.initialize(ctj.scan, quotes, sep, decimalSeparatorPoint);

		return ctj._parse(quotes, sep, decimalSeparatorPoint);
	}			
			
	private CsvRows _parse(char quotes, char sep, boolean decimalSeparatorPoint) {
		token = scan.nextToken();
		CsvRows csvStore = new CsvRows();
		CsvRow rowFirst = null; 

		// Leemos las filas
		while (token != Scanner.EOF) {			
			// Leemos la fila
			CsvRow row = new CsvRow(new CsvConfig(quotes, sep, decimalSeparatorPoint));
			do {
				Object value = readField();
				row.add(value);
			} while (pareado(TokensCsv.SEP));
			
			// Comprobamos si es la primera fila
			if (rowFirst == null) {
				rowFirst = row;
			}
			
			if (row.size() != rowFirst.size()) {
				throw new CsvDbQSintaxException("Número de columnas discordante con la línea "
						+ "previa, en la línea " + scan.tokenLin() + ", columna " +
						scan.tokenCol() + ".");
			}
			
			if (token == Scanner.EOL) {
				token = scan.nextToken();
			}
			
			// Agregamos la fila al CSV
			csvStore.add(row);			
		}		

		return csvStore;
	}

	// Lee el siguiente campo de la fila actual
	private Object readField() {
		List<Integer> tokens = new ArrayList<>();
		String field = "";
		double num = 0.0; // Guarda el número en caso de que se lea uno
		while (token != TokensCsv.SEP && token != Scanner.EOL && token != Scanner.EOF) {
			tokens.add(token);
			String s = scan.lex();
			if (token == Scanner.STRING) {
				s = scan.lexString();
			} else if (token == Scanner.NUMBER) {
				num = scan.getNum();
			}
			field += s;
			token = scan.nextToken();
		}

		// Podemos devolver una cadena, un número, true, false o null
		if (tokens.size() == 1 && tokens.get(0) == Scanner.NUMBER) {
			return num;
		} else if (tokens.size() == 2 && tokens.get(0) == TokensCsv.MINUS && tokens.get(1) == Scanner.NUMBER) {
			return -num;
		} else if (tokens.size() == 1 && tokens.get(0) == TokensCsv.TRUE) {
			return true;
		} else if (tokens.size() == 1 && tokens.get(0) == TokensCsv.FALSE) {
			return false;
		} else if (tokens.size() == 1 && tokens.get(0) == TokensCsv.NULL) {
			return null;
		} else {
			return field;
		}
	}	

	private boolean pareado(int expected) {
		if (token == expected) {
			token = scan.nextToken();
			return true;
		} else
			return false;
	}			
}
