package es.facite.csvdbq.qlcsv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 Procesa un script de inserción en tabla, por ejemplo:
 * INSERT INTO personas(nombre, edad, fecha_nac) values
 * 		('miguel', 53, '1970-10-27'),
 * 		('eva', 29, '1994-10-27').
 */
public class Insert {
	private TextScanner scan;
	private int token;
	private CsvConfig config;

	public Insert(CsvConfig dbManager) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = dbManager;
	}

	/**
	 * Ejecuta el comando 'SELECT' y delvuelve una colección de filas.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public int execute(String script) {
		prepareScanner(script);
		match(Tokens.INSERT, "INSERT");
		match(Tokens.INTO, "INTO");		
		
		var tableName = getTableName();
		var pathCsv = config.composePath(tableName);
		
		try (var csvHeaders = new CsvFileHeaders(tableName, config)) {			
			match(Tokens.OPEN_PARENT, "(");		
			List<Integer> fieldIndexes = new ArrayList<>();
			do {
				if (token != Scanner.IDENT) {
					throw new CsvDbQSintaxException("un nombre de campo de tabla", scan);
				}
				String name = scan.lex();
				token = scan.nextToken();
				// ¿Existe el índice del campo en base de datos?
				var optInt = IntStream.range(0, csvHeaders.size())
					.filter(index -> csvHeaders.getLowerCase(index).equals(name))
					.findFirst();
				if (optInt.isEmpty())
					throw new CsvDbQSintaxException("El campo '" + name + "' no existe en la tabla.");			
				fieldIndexes.add(optInt.getAsInt());
				if (!matchIf(Tokens.COMMA)) break;
			} while (true);
			
			match(Tokens.CLOSED_PARENT, ")");
			match(Tokens.VALUES, "VALUES");		
					
			
			// Cierra el recurso CsvHeaders para evitar acceso concurrente
			csvHeaders.close();
			
			// Inserta las filas
			int rowsInserted = 0;
			try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathCsv),
					StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {	
				do {
					match(Tokens.OPEN_PARENT, "(");
					// Inicializa la fila con todos los campos a 'null'
					var row = new CsvRow(config);
					for (int i = 0; i < csvHeaders.size(); i++) {
						row.add(null);
					}
					
					for (int i = 0; i < fieldIndexes.size(); i++) {
						if (i > 0) {
							match(Tokens.COMMA, ",");
						}
						switch(scan.getTokenClass()) {
						case Scanner.NUMBER:
							row.set(fieldIndexes.get(i), scan.getNum());					
							break;
						case Scanner.STRING:
							row.set(fieldIndexes.get(i), scan.lexString());					
							break;
						case Scanner.KEYWORD:
							if (token == Tokens.TRUE || token == Tokens.FALSE) {
								row.set(fieldIndexes.get(i), token == Tokens.TRUE);
							} else if (token != Tokens.NULL) {
								throw new CsvDbQSintaxException("true, false, o null", scan);
							}
							break;
						}
						token = scan.nextToken();
					}
					match(Tokens.CLOSED_PARENT, ")");
					
					// Escribe la fila en la salida siempre precedida de una nueva línea, ya que el
					// fichero tiene al menos la cabecera o más filas existentes
					bw.write(TokensCsv.NL);				
					bw.write(config.getCipher().isEncrypted() ? row.toEncrypted() : row.toString());
					rowsInserted++;
	
					// ¿Hay más filas?
					if (!matchIf(Tokens.COMMA)) {
						break;
					}
				} while (true);
			} catch (IOException e) {	
				throw new CsvDbQException(e);
			}
			match(Scanner.EOF, "fin del script");
			
			return rowsInserted;		
		}
	}

	private void match(int expected, String str) {
		if (token == expected) {
			token = scan.nextToken();
		} else {
			throw new CsvDbQSintaxException(str, scan);
		}
	}

	private boolean matchIf(int expected) {
		if (token == expected) {
			token = scan.nextToken();
			return true;
		} else
			return false;
	}
	
	private void prepareScanner(String script) {
		scan.setTextIn(script);
		token = scan.nextToken();		
		
		script = "";
		while (token != Scanner.EOF) {
			if (script.length() > 0) {
				script += " ";
			}
			String lex = scan.lex();
			if (token == Scanner.IDENT) {
				lex = lex.toLowerCase();
			}
			script += lex;
			token = scan.nextToken();
		}
		scan.setTextIn(script);
		token = scan.nextToken();		
	}
	
	private String getTableName() {
		if (token != Scanner.IDENT && token != Scanner.STRING) {
			throw new CsvDbQSintaxException("nombre de tabla", scan);
		}
		final String tableName = token == Scanner.IDENT ? scan.lex() : scan.lexString();
		token = scan.nextToken();
		return tableName;
	}
}
