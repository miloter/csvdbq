package es.facite.csvdbq.qlcsv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.stream.IntStream;

import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.iterator.CsvHeaders;
import es.facite.csvdbq.util.Util;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesa un script de actualización de tabla, por ejemplo: UPDATE personas SET
 * nombre = 'miguel', fecha_nac = '1970-10-27' where edad = 53.
 */
public class Update {
	private TextScanner scan;
	private int token;
	private CsvConfig config;
	private Evaluator evaluator;

	public Update(CsvConfig config) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = config;
		evaluator = new Evaluator(this.config);
	}

	/**
	 * Ejecuta el comando 'UPDATE' y delvuelve el número de filas actualizadas.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public int execute(String script) {
		prepareScanner(script);

		// "UPDATE" table
		match(Tokens.UPDATE, "UPDATE");
		var tableName = getTableName();
		var pathCsv = config.composePath(tableName);
		
		try (var csvHeaders = new CsvFileHeaders(tableName, config)) {
			var iterator = csvHeaders.getIterator();
	
			// SET field1 = expr1 {"," field "=" expr}
			match(Tokens.SET, "SET");
			var updateExpressions = new UpdateExpressions();
			do {
				updateExpressions.add(updateExpression(csvHeaders));
				if (!matchIf(Tokens.COMMA)) break;
			} while (true);
	
			// Sección WHERE
			String where = null;
			if (matchIf(Tokens.WHERE)) {
				where = "";
				while (token != Tokens.SEMICOLON && token != Scanner.EOF) {
					if (where.length() != 0) {
						where += " ";
					}
					where += scan.lex();
					token = scan.nextToken();
				}
			}
	
			// Final de la query
			matchIf(Tokens.SEMICOLON);
			match(Scanner.EOF, "fin del script");
		
			// La salida se manda a un fichero temporal
			var pathCsvTemp = Util.createTempFile();

			int updatedRows = 0;
			try (BufferedWriter bw = Files.newBufferedWriter(pathCsvTemp, StandardCharsets.UTF_8)) {						
				bw.write("\ufeff"); // BOM de UTF-8 para Office de Microsoft
				
				// Escribe la fila de cabeceras			
				bw.write(config.getCipher().isEncrypted() ? csvHeaders.toEncrypted() : csvHeaders.toString());
			
				var mapHeadersToValues = new HashMap<String, Object>();			
				while (iterator.hasNext()) {
					var row = iterator.next();
		
					// Establece los valores de cada campo para evaluar el
					// "WHERE" o las expresiones "SET"
					for (int i = 0; i < row.size(); i++) {
						mapHeadersToValues.put(csvHeaders.getLowerCase(i), row.get(i));
					}				
					evaluator.setVars(mapHeadersToValues);
					if (Util.evalWhere(evaluator, where)) {				
						for (var ue : updateExpressions) {
							row.set(ue.getIndex(), evaluator.eval(ue.getExpression()));
						}			
						updatedRows++;
					}
					
					// Agrega la fila al resultado
					bw.write(TokensCsv.NL);
					bw.write(config.getCipher().isEncrypted() ? row.toEncrypted() : row.toString());
					
				}
			} catch (IOException e) {
				throw new CsvDbQException(e);
			}
			
			// Cierra el recurso CsvHeaders para evitar acceso councurrente
			csvHeaders.close();
			
			// Aplica la actualización a nivel de fichero
			try {
				Files.move(pathCsvTemp, Path.of(pathCsv), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {			
				throw new CsvDbQException(e);
			}
	
			return updatedRows;
		}
	}

	private UpdateExpression updateExpression(CsvHeaders csvHeaders) {
		// field "=" expr		
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
		int index = optInt.getAsInt();
		
		match(Tokens.ASIG, "=");
		
		String expression = "";
		int sumParenth = 0;
		do {
			if (token == Tokens.OPEN_PARENT)
				sumParenth++;
			if (token == Tokens.CLOSED_PARENT)
				sumParenth--;

			expression += scan.lex();
			token = scan.nextToken();
		} while ((token != Tokens.COMMA || sumParenth != 0) && token != Tokens.WHERE
				&& token != Scanner.EOF);

		return new UpdateExpression(index, expression);
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
