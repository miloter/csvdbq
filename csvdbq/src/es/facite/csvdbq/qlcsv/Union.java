package es.facite.csvdbq.qlcsv;

import java.io.IOException;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesa un script de unir tablas que tengan el mismo número de campos:
 * UNION(tabla1, tabla2). Tanto tabla1 como tabla2 pueden ser una SELECT
 * un JOIN, una UNION o una tabla en disco. Las cabeceras de la tabla unida
 * serán las de la primera tabla.
 */
public class Union {
	private TextScanner scan;
	private int token;
	private CsvConfig config;

	public Union(CsvConfig config) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = config;
	}

	/**
	 * Ejecuta el comando UNION y devuelve la tabla resultado.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public ResultSelect execute(String script) {
		scan.setTextIn(script);
		token = scan.nextToken();

		// "UNION" "(" 		(select1 | join1 | union1 | tabla1);
		//					(select2 | join2 | union2 | tabla2) ")"
		match(Tokens.UNION, "UNION");
		match(Tokens.OPEN_PARENT, "(");

		ResultSelect rs1 = null;
		ResultSelect rs2 = null;

		if (token == Tokens.SELECT) {
			rs1 = new Select(config).execute(balancedExpression());
		} else if (token == Tokens.JOIN) {
			rs1 = new Join(config).execute(balancedExpression());
		} else if (token == Tokens.UNION) {
			rs1 = new Union(config).execute(balancedExpression());
		} else {
			rs1 = new Select(config).execute("select * from " + getTableName());
		}
		match(Tokens.SEMICOLON, ";");
		if (token == Tokens.SELECT) {
			rs2 = new Select(config).execute(balancedExpression());
		} else if (token == Tokens.JOIN) {
			rs2 = new Join(config).execute(balancedExpression());
		} else if (token == Tokens.UNION) {
			rs2 = new Union(config).execute(balancedExpression());
		} else {
			rs2 = new Select(config).execute("select * from " + getTableName());
		}

		if (rs1.getHeaders().size() != rs2.getHeaders().size()) {
			throw new CsvDbQException("Solo se pueden unit tablas con el mismo número de campos.");
		}

		match(Tokens.CLOSED_PARENT, ")");
		match(Scanner.EOF, "fin del script");

		// Prepara una cabecera común para la salida
		CsvRow headers = new CsvRow(config);
		for (var header : rs1.getHeaders()) {
			headers.add(header);
		}

		// Prepara las filas del resultado
		CsvRows rows = new CsvRows();
		for (var row : rs1.getRows()) {
			rows.add(row);
		}
		for (var row : rs2.getRows()) {
			rows.add(row);
		}

		return new ResultSelect(headers, rows);
	}
		
	private void match(int expected, String str) {
		if (token == expected) {
			token = scan.nextToken();
		} else {
			throw new CsvDbQSintaxException(str, scan);
		}
	}

	private String getTableName() {
		if (token != Scanner.IDENT && token != Scanner.STRING) {
			throw new CsvDbQSintaxException("nombre de tabla", scan);
		}
		final String tableName = scan.lex();
		token = scan.nextToken();
		return tableName;
	}

	private String balancedExpression() {
		String expression = "";
		int sumParenth = 0;
		do {
			if (expression.length() > 0) {
				expression += " ";
			}
			if (token == Tokens.OPEN_PARENT)
				sumParenth++;
			if (token == Tokens.CLOSED_PARENT)
				sumParenth--;

			expression += scan.lex();
			token = scan.nextToken();
		} while (
				!(sumParenth == 0 &&
					(token == Tokens.SEMICOLON || token == Tokens.CLOSED_PARENT)) &&
				token != Scanner.EOF);

		return expression;
	}	
}
