package es.facite.csvdbq.qlcsv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.text.CsvScanner;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesa un script de juntar tablas mediante un campo común, por ejemplo:
 * JOIN(tabla1, tabla2, id). Tanto tabla1 como tabla2 pueden ser una SELECT
 * un JOIN, una UNION, o una tabla en disco. El campo de union solo aparecerá
 * una vez en la tabla resultado. 
 */
public class Join {
	private TextScanner scan;
	private int token;
	private CsvConfig config;

	public Join(CsvConfig config) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = config;
	}

	/**
	 * Ejecuta el comando JOIN y devuelve la tabla resultado.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public ResultSelect execute(String script) {
		scan.setTextIn(script);
		token = scan.nextToken();

		// "JOIN" "(" 	(select1 | join1 | union1 | tabla1);
		//				(select2 | join2 | union2 | tabla2); field ")"
		match(Tokens.JOIN, "JOIN");
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
		match(Tokens.SEMICOLON, ";");
		final String fieldName = getFieldName();

		final int ih1 = indexHeader(fieldName, rs1.getHeaders());
		final int ih2 = indexHeader(fieldName, rs2.getHeaders());

		if (ih1 < 0 || ih2 < 0) {
			throw new CsvDbQException("El campo '" + fieldName + "' no es común en ambas tablas.");
		}

		match(Tokens.CLOSED_PARENT, ")");
		match(Scanner.EOF, "fin del script");

		// Prepara una cabecera común para la salida
		CsvRow headers = new CsvRow(config);
		for (var header : rs1.getHeaders()) {
			headers.add(header);
		}
		for (int i = 0; i < rs2.getHeaders().size(); i++) {
			if (i == ih2)
				continue;
			headers.add(rs2.getHeaders().get(i));
		}

		// Prepara las filas del resultado
		CsvRows rows = new CsvRows();

		// Mapa para buscar la fila que tiene la columna coincidente
		Map<Object, CsvRow> map = new HashMap<>();

		// Recorremos la tabla izquierda con menor o igual número de filas
		if (rs1.size() <= rs2.size()) {
			// Ponemos la tabla con más filas en el mapa
			for (var row : rs2.getRows()) {
				map.put(row.get(ih2), row);
			}

			for (var row : rs1.getRows()) {
				var row2 = map.get(row.get(ih1));
				if (row2 != null) {
					// Se agrega al resultado la concatenación de ambas filas
					var newRow = new CsvRow(config);
					for (int i = 0; i < row.size(); i++) {
						newRow.add(row.get(i));
					}
					for (int i = 0; i < row2.size(); i++) {
						if (i == ih2)
							continue;
						newRow.add(row2.get(i));
					}
					rows.add(newRow);
				}
			}
		} else { // Recorremos la tabla derecha
			// Ponemos la tabla con más filas en el mapa
			for (var row : rs1.getRows()) {
				map.put(row.get(ih1), row);
			}

			for (var row : rs2.getRows()) {
				var row1 = map.get(row.get(ih1));
				if (row1 != null) {
					// Se agrega al resultado la concatenación de ambas filas
					var newRow = new CsvRow(config);
					for (int i = 0; i < row1.size(); i++) {
						newRow.add(row1.get(i));
					}
					for (int i = 0; i < row.size(); i++) {
						if (i == ih2)
							continue;
						newRow.add(row.get(i));
					}
					rows.add(newRow);
				}
			}
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
		} while (!(sumParenth == 0 && token == Tokens.SEMICOLON) && token != Scanner.EOF);

		return expression;
	}

	private String getFieldName() {
		if (token != Scanner.IDENT) {
			throw new CsvDbQSintaxException("nombre de campo", scan);
		}
		final String fieldName = scan.lex();
		token = scan.nextToken();

		return fieldName;
	}

	/**
	 * Devuelve el índice de una cabecera coincidente con alguno de los valores de
	 * esta fila.
	 * 
	 * @param header
	 * @param row
	 * @return
	 */
	public int indexHeader(String header, CsvRow row) {
		for (int i = 0; i < row.size(); i++) {
			String h = row.get(i).toString();
			// Normaliza la cabecera
			if (!CsvScanner.isIdent(h)) {
				h = "[" + h + "]";
			}
			if (h.equalsIgnoreCase(header))
				return i;
		}

		return -1;
	}
}
