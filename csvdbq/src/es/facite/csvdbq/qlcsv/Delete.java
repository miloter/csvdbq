package es.facite.csvdbq.qlcsv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.util.Util;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesa un script de eliminación de filas de una tabla, por ejemplo:
 * DELETE FROM personas WHERE edad > 120.S
 */
public class Delete {
	private TextScanner scan;
	private int token;
	private CsvConfig config;
	private Evaluator evaluator;

	public Delete(CsvConfig config) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = config;
		evaluator = new Evaluator(this.config);
	}

	/**
	 * Ejecuta el comando 'DELETE' y delvuelve el número de filas eliminadas.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public int execute(String script) {
		prepareScanner(script);

		// "DELETE FROM" table		
		match(Tokens.DELETE, "DELETE");
		match(Tokens.FROM, "FROM");		
		var tableName = getTableName();
		
		var pathCsv = config.composePath(tableName);

		int rowsDeleted = 0;
		try (var csvHeaders = new CsvFileHeaders(tableName, config)) {
			var iterator = csvHeaders.getIterator();

			// ["WHERE" expression]
			String where = getWhere();								
	
			// Final de la query
			matchIf(Tokens.SEMICOLON);
			match(Scanner.EOF, "fin del script");	
			
			// La salida se manda a un fichero temporal
			var pathCsvTemp = Util.createTempFile();
						
			try (BufferedWriter bw = Files.newBufferedWriter(pathCsvTemp, StandardCharsets.UTF_8)) {
				bw.write("\ufeff"); // BOM de UTF-8 para Office de Microsoft
				
				// Escribe la fila de cabeceras				
				bw.write(config.getCipher().isEncrypted() ? csvHeaders.toEncrypted() : csvHeaders.toString());
			
				var mapHeadersToValues = new HashMap<String, Object>();
				// Recorremos todos los registros
				while (iterator.hasNext()) {
					var row = iterator.next();
					
					// Si el WHERE existe, establece los valores de cada campo para evaluarlo
					if (where != null) {
						for (int i = 0; i < row.size(); i++) {
							mapHeadersToValues.put(csvHeaders.getLowerCase(i), row.get(i));
						}
						evaluator.setVars(mapHeadersToValues);
					}
					
					if (Util.evalWhere(evaluator, where)) {
						rowsDeleted++;
					} else {					
						bw.write(TokensCsv.NL);					
						bw.write(config.getCipher().isEncrypted() ?
								row.toEncrypted() : row.toString());					
					}			
				}							
			} catch (IOException e) {
				throw new CsvDbQException(e);
			}
			
			// Cierra el recurso CsvHeaders para evitar acceso concurrente
			csvHeaders.close();
			
			// Aplica la actualización a nivel de fichero
			try {
				Files.move(pathCsvTemp, Path.of(pathCsv), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {			
				throw new CsvDbQException(e);
			}				
		}		
			
		return rowsDeleted;						
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
	
	private String getWhere() {		
		if (!matchIf(Tokens.WHERE)) return null;
		
		String where = "";
		do {			
			if (where.length() > 0) {
				where += " ";
			}
			where += scan.lex();
			token = scan.nextToken();
		} while (token != Tokens.SEMICOLON && token != Scanner.EOF);		
		
		return where;
	}
}
