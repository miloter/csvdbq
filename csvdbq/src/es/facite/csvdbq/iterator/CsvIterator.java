package es.facite.csvdbq.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.text.CsvFileScanner;
import es.facite.text.CsvScanner;
import es.facite.text.Scanner;

public class CsvIterator implements Iterable<CsvRow>, AutoCloseable {
	private CsvScanner scan;
	private int token;
	private CsvRow currentRow;
	private CsvRow rowFirst;
	private CsvConfig config;

	public CsvIterator(CsvScanner scan, CsvConfig config) {
		this.scan = scan;
		this.config = config;
		TokensCsv.initialize(scan, config.getQuotes(), config.getSep(), config.isDecimalSeparatorPoint());
	}

	@Override
	public Iterator<CsvRow> iterator() {
		return new IteratorImpl();
	}

	private void start() {
		token = scan.nextToken();
		currentRow = nextRow();
	}

	private CsvRow nextRow() {
		if (token == Scanner.EOF)
			return null;

		// Leemos la fila
		CsvRow row = new CsvRow(config);
		do {
			Object value;
			if (config.getCipher().isEncrypted() && !config.getCipher().isReadPlainText()) {
				final String str = config.getCipher().decrypt(readFieldAsString());
				if (str.equals("null")) {
					value = null;
				} else if (str.equals("true")) {
					value = true;
				} else if (str.equals("false")) {
					value = false;
				} else if (str.isEmpty() || str.isBlank()) {
					value = str;
				} else if (str.charAt(0) < '0' || str.charAt(0) > '9') {
					value = str;
				} else {
					try {
						value = Double.parseDouble(str);						
					} catch (NumberFormatException e) {
						value = str;
					}
				}
			} else {
				value = readField();
			}
			row.add(value);
		} while (pareado(TokensCsv.SEP));

		// Anotamos la primera fila para comprobar el número de columnas
		if (currentRow == null) {
			rowFirst = row;
		}

		if (row.size() != rowFirst.size()) {
			throw new CsvDbQSintaxException("Número de columnas discordante con la línea " + "previa, en la línea "
					+ scan.tokenLin() + ", columna " + scan.tokenCol() + ".");
		}

		if (token == Scanner.EOL) {
			token = scan.nextToken();
		}

		return row;
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

	// Lee el siguiente campo de la fila actual como una cadena
	private String readFieldAsString() {		
		String field = "";
		
		while (token != TokensCsv.SEP && token != Scanner.EOL && token != Scanner.EOF) {			
			if (token == Scanner.STRING) {
				field += scan.lexString();
			} else {
				field += scan.lex();
			}			
			token = scan.nextToken();
		}

		return field;		
	}

	private boolean pareado(int expected) {
		if (token == expected) {
			token = scan.nextToken();
			return true;
		} else
			return false;
	}

	private class IteratorImpl implements Iterator<CsvRow> {
		public IteratorImpl() {
			start();
		}

		@Override
		public boolean hasNext() {
			return currentRow != null;
		}

		@Override
		public CsvRow next() {
			CsvRow row = currentRow;
			currentRow = nextRow();

			return row;
		}
	}

	@Override
	public void close() {
		if (scan instanceof CsvFileScanner fs) {
			fs.dispose();
		}
		currentRow = null;
		scan = null;
		rowFirst = null;
	}
}
