package es.facite.csvdbq.qlcsv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.iterator.CsvHeaders;
import es.facite.csvdbq.util.Util;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesa un script de selección de tabla, por ejemplo: SELECT edad, fecha_nac
 * FROM personas WHERE edad > 47.
 */
public class Select {
	private TextScanner scan;
	private int token;
	private CsvConfig config;
	private Evaluator evaluator;

	public Select(CsvConfig config) {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		this.config = config;
		evaluator = new Evaluator(this.config);
	}

	/**
	 * Ejecuta el comando 'SELECT' y delvuelve una colección de filas.
	 * 
	 * @param script
	 * @throws IOException
	 */
	public ResultSelect execute(String script) {
		prepareScanner(script);

		// "SELECT"		
		match(Tokens.SELECT, "SELECT");
		
		// ["DISTINCT"]
		var distinct = matchIf(Tokens.DISTINCT);
		
		 // "*" | (expr1 ["as" label1] {, exprn ["as" labeln})
		var selectExpressions = new SelectExpressions();
		var selectWildcard = false;
		if (token != Tokens.MUL) {
			do {				
				selectExpressions.add(selectExpression());
				if (token != Tokens.COMMA) break;
				token = scan.nextToken();
			} while (true);
		} else {
			selectWildcard = true;
			token = scan.nextToken();
		}

		// FROM ("JOIN" "(" ... ")" | "UNION" "(" ... ")" | table_name)
		match(Tokens.FROM, "FROM");
		String tableName = null;
		ResultSelect resultSelect = null;
		if (token == Tokens.JOIN) {
			resultSelect = new Join(config).execute(balancedExpression());
		} else if (token == Tokens.UNION) {
			resultSelect = new Union(config).execute(balancedExpression());
		} else {
			tableName = getTableName();
		}
		
		try (var csvHeaders = (tableName != null) ?
				new CsvFileHeaders(tableName, config) : new CsvHeaders(resultSelect)) {
			var iterator = csvHeaders.getIterator();
			
			// Agregamos las expresiones select si se usó el comodín '*'
			if (selectWildcard) {
				for (var h : csvHeaders.getHeadersNormalize()) {
					selectExpressions.add(new SelectExpression(h));
				}
			}
			
			// ["WHERE" expression]
			final String where = getWhere();
			
			// ["GROUP" "BY" (n | label) {, (n | label)}]
			var groupBy = getGroupBy(selectExpressions);		
			
			// ["HAVING" expression]
			var having = getHaving();
			// Restricción del uso de 'HAVING'
			if (having != null && groupBy.isEmpty()) {
				throw new CsvDbQException("'HAVING' solo puede usarse si se precede de 'GROUP BY'.");
			}		
			
			// [ORDER BY (n | label) [ASC | DESC] {, (n | label) [ASC | DESC]}]
			var orderBy = getOrderBy(selectExpressions);
	
			// [LIMIT [offset, ] count]
			var limitExpression = getLimitExpression();

			// Final de la query
			match(Scanner.EOF, "fin del script");		
			
			// Una vez analizada la "SELECT", se extraen las funciones de agregado y se
			// configura cada expresión "SELECT"
			var aggFunctions = new AggregateFunctions();
			for (var se : selectExpressions) {
				var afs = extractAggFunctions(se.getExpression());
				aggFunctions.addAll(afs);
				se.setAggFunctions(afs.size() > 0);
			}
			var hasAggregate = aggFunctions.size() > 0;
			var hasExpressions = selectExpressions.stream().anyMatch(se -> !se.isAggFunctions());
		
			// Restricciones del uso de 'DISTINCT'
			if (hasAggregate && distinct) {
				throw new CsvDbQException("Solo se puede usar 'DISTINCT' "
						+ "cuando no hay funciones de agregado.");
			}
			
			// Restricciones del uso de 'GROUP BY'
			if (hasAggregate && hasExpressions && groupBy.isEmpty()) {			
				throw new CsvDbQException("No se permiten otras expresiones sin aparecer en una "
						+ "claúsula GROUP BY cuando hay funciones de agregado.");
			}				
			if (groupBy.size() > 0 && !hasAggregate) {
				throw new CsvDbQException("Solo se puede usar GROUP BY cuando existen "
						+ "funciones de agregado.");
			}				
	
			// Prepara la fila de cabeceras
			var headers = getHeaders(csvHeaders, selectExpressions, selectWildcard);		
			// Se devuelve una lista personalizada de registros
			var rows = new CsvRows();		
			// Mapea cabeceras y sus valores
			var mapHeadersToValues = new HashMap<String, Object>();		
			// Para comprobar de manera rápida la existencia de filas agrupadas
			Map<String, CsvRow> mapGroupRow = new HashMap<>();
			// Para calcular los acumuladores de una fila concreta
			Map<CsvRow, AggregateFunctions> mapRowAccum = new HashMap<>();
	
			while (iterator.hasNext()) {
				var row = iterator.next();
	
				// Establece los valores de cada campo para evaluar el where o las expresiones
				// SELECT
				for (int i = 0; i < row.size(); i++) {
					mapHeadersToValues.put(csvHeaders.getLowerCase(i), row.get(i));
				}
				evaluator.setVars(mapHeadersToValues);
				if (!Util.evalWhere(evaluator, where))
					continue;
				
				// Si existe DISTINCT se debe verificar que no exista la fila
				if (distinct) {
					final String group = getGroupString(selectExpressions);
					if (mapGroupRow.containsKey(group)) {
						continue;
					} else {
						mapGroupRow.put(group, row);
					}
				}		
	
				// Calcula y agrega la fila resultado si no se uso el wildcard '*' y la
				// consulta no tiene funciones de agregado
				if (!(selectWildcard || hasAggregate)) {
					row = new CsvRow(config);
					for (var se : selectExpressions) {
						row.add(evaluator.eval(se.getExpression()));					
					}
				}
	
				// Si es una consulta de agregado calcula el valor actual de las funciones
				if (hasAggregate) {
					genGroupedRow(selectExpressions, mapGroupRow, mapRowAccum, aggFunctions, rows);				
				} else {				
					rows.add(row);
				}
			}
		
			// Si es una consulta de agregado calculamos y añadimos el resultado
			if (hasAggregate) {
				finalizeAggCalc(mapRowAccum, selectExpressions);						
				
				// Si hay claúsula "HAVING" se filtran las filas agrupadas
				if (having != null) {
					rows = filterByHaving(selectExpressions, having, rows);				
				}
			}
	
			// Ordenamos el resultado
			orderResult(orderBy, rows);		
			
			// Comprobamos si hay que aplicar límite a la devolución
			if (limitExpression.getOffset() > 0 || limitExpression.getCount() < rows.size()) {
				rows = new CsvRows(rows, Math.min(rows.size(), limitExpression.getOffset()),
						Math.min(limitExpression.getOffset() + limitExpression.getCount(), rows.size()));
			}
	
			return new ResultSelect(headers, rows);
		}
	}
	
	private CsvRows filterByHaving(SelectExpressions selectExpressions, String having, CsvRows rows) {		
		CsvRows rowsHaving = new CsvRows();				
		// Para mapear cada expresión de la "SELECT" a los valores actuales en la fila
		// Para mapear las expresiones "SELECT" a variables normales por índice
		var vars = new ArrayList<String>();
		// Para mapear las variables a sus valores
		var mapVarToValue = new HashMap<String, Object>();
		
		// Mapea las expresiones a variables por índice
		for (int i = 0; i < selectExpressions.size(); i++) {
			vars.add("v" + Double.valueOf(Math.random()).toString().substring(2));				
		}
		
		// Crea un having sustituto para las evaluaciones
		String having2 = having;
		for (int i = 0; i < selectExpressions.size(); i++) {
			having2 = having2.replace(selectExpressions.get(i).getExpression(), vars.get(i));
		}
		
		// Comienza las evaluaciones
		for(var row : rows) {
			// Establece los valores de cada campo para evaluar el having
			for (int i = 0; i < row.size(); i++) {						
				mapVarToValue.put(vars.get(i), row.get(i));
			}
			evaluator.setVars(mapVarToValue);
			if (Util.isTrue(evaluator.eval(having2))) {
				rowsHaving.add(row);
			}						
		}
		
		return rowsHaving;
	}

	private String getGroupString(SelectExpressions selectExpressions) {
		var sb = new StringBuilder(); 		
		
		for (var se : selectExpressions) {
			if (!se.isAggFunctions()) {
				var value = evaluator.eval(se.getExpression());
				sb.append(value != null ? value.toString().toLowerCase() : "null");						
			}												
		}
		
		return sb.toString();
	}
	
	private void genGroupedRow(SelectExpressions selectExpressions, Map<String, CsvRow> mapGroupRow,
			Map<CsvRow, AggregateFunctions> mapRowAccum, AggregateFunctions aggFunctions, CsvRows rows) {		
		// Obtiene la fila de agrupación				
		final String group = getGroupString(selectExpressions);
		var row = mapGroupRow.get(group); 
		
		// Si la agrupación no existe genera una nueva
		if (row == null) {
			row = new CsvRow(config);
			for (var se : selectExpressions) {
				if (se.isAggFunctions()) {
					row.add(null);
				} else {
					row.add(evaluator.eval(se.getExpression()));
				}
			}
			mapGroupRow.put(group, row);
			
			// Agregamos la fila a la salida
			rows.add(row);
			
			// Generamos una nueva entrada para el cálculo de acumuladores
			var afs = new AggregateFunctions();
			for (var af : aggFunctions) {
				var af2 = new AggregateFunction(af.getAggFuncId(), af.getAggregateExpression());
				afs.add(af2);
			}
			mapRowAccum.put(row, afs);
		}
		// Actualiza el valor de los acumuladores en función de la filla actual
		var afs = mapRowAccum.get(row);
		for (var af : afs) {
			calcAggregateFunction(af);
		}
	}

	private CsvRow getHeaders(CsvHeaders csvHeaders, SelectExpressions selectExpressions,
			boolean selectWildcard) {
		var headers = new CsvRow(config);
		
		if (selectWildcard) {
			for (var h : csvHeaders) {
				headers.add(h);
			}
		} else {
			for (var se : selectExpressions) {
				headers.add(se.getLabel());
			}
		}
		
		return headers;
	}
		
	private void finalizeAggCalc(Map<CsvRow, AggregateFunctions> mapRowAccum,
			SelectExpressions selectExpressions) {
		// Establecemos los valores en cada fila de agrupación
		for(var entry : mapRowAccum.entrySet()) { 
			// Calculamos los promedios en las funciones 'AVG'
			for (var af : entry.getValue()) {
				if (af.getAggFuncId() == Tokens.AVG && af.getCountNotNull() > 0) {
					af.setValue((double) af.getValue() / (double) af.getCountNotNull());
				}				
			}			

			// Calculamos el valor de la fila resultante
			// Borramos las variables para evitar cálculos erróneos
			evaluator.setVars(new HashMap<String, Object>());				
			// Sustituimos en las expresiones los literales de agregado por su valor calculado
			for (int i = 0; i < selectExpressions.size(); i++) {
				var se = selectExpressions.get(i);
				if (!se.isAggFunctions()) continue;
				
				String expr = se.getExpression();
				for (var af: entry.getValue()) {
					var value = af.getValue();
					String strValue;
					if (value instanceof Double dbl) {
						strValue = dbl.toString();		
					} else if (value instanceof String str) {
						strValue = "'" + str + "'";
					} else if (value instanceof Boolean bln) {
						strValue = bln.toString();
					} else {
						strValue = "null";
					}
					expr = expr.replace(af.getAggregateExpression().getExpression(), strValue);					
				}
				entry.getKey().set(i, evaluator.eval(expr));
			}
		}
	}
		
	private void orderResult(OrderByExpressions orderBy, CsvRows rows) {
		for (int i = orderBy.size() - 1; i >= 0; i--) {
			var order = orderBy.get(i);
			rows.sort(order.getIndex(), order.isAsc());
		}
	}
	
	private void calcAggregateFunction(AggregateFunction af) {
		Object value;
		
		af.incCount();
		
		if (af.getAggFuncId() == Tokens.COUNT && af.getAggregateExpression().getInnerExpression().equals(" * ")) {
			value = null;
		} else {
			value = evaluator.eval(af.getAggregateExpression().getInnerExpression());
		}
		
		if (value != null) {
			af.incCountNotNull();
		}
		
		switch (af.getAggFuncId()) {
		case Tokens.COUNT:
			if (af.getAggregateExpression().getInnerExpression().equals(" * ")) {
				af.setValue((double) af.getCount());
			} else {				
				if (value != null) {
					if (af.getValue() != null) {
						af.setValue((double) af.getValue() + 1.0);
					} else {
						af.setValue(1.0);
					}
				}
			}
			break;
		case Tokens.SUM:
		case Tokens.AVG:
			if (value != null) {
				if (af.getValue() != null) {
					af.setValue((double) af.getValue() + (double) value);
				} else {
					af.setValue((double) value);
				}				
			}
			break;
		case Tokens.MIN:
		case Tokens.MAX:
			if (value != null) {
				if (af.getValue() != null) {
					if (af.getAggFuncId() == Tokens.MIN) {
						if (value instanceof Double dbl) {
							if (dbl < (double) af.getValue()) {
								af.setValue(dbl);
							}
						} else if (value instanceof String str) {
							if (str.compareToIgnoreCase((String) af.getValue()) < 0) {
								af.setValue(str);
							}
						} else { // Boolean
							var bln = (Boolean) value;
							if (bln.compareTo((Boolean) af.getValue()) < 0) {
								af.setValue(bln);
							}
						}
					} else {
						if (value instanceof Double dbl) {
							if (dbl > (double) af.getValue()) {
								af.setValue(dbl);
							}
						} else if (value instanceof String str) {
							if (str.compareToIgnoreCase((String) af.getValue()) > 0) {
								af.setValue(str);
							}
						} else { // Boolean
							var bln = (Boolean) value;
							if (bln.compareTo((Boolean) af.getValue()) > 0) {
								af.setValue(bln);
							}
						}
					}
				} else {
					af.setValue(value);
				}
			}
			break;
		}
	}

	private LimitExpression getLimitExpression() {
		int offset = 0;
		int count = Integer.MAX_VALUE;

		if (!matchIf(Tokens.LIMIT))
			return new LimitExpression(offset, count);
		if (token != Scanner.NUMBER)
			throw new CsvDbQSintaxException("un número entero no negativo", scan);

		Double num = scan.getNum();
		if (!Util.isInteger(num, 0, Integer.MAX_VALUE))
			throw new CsvDbQSintaxException("un número entero en el rango [0, " + Integer.MAX_VALUE + "]", scan);
		count = num.intValue();
		token = scan.nextToken();
		if (matchIf(Tokens.COMMA)) {
			if (token != Scanner.NUMBER)
				throw new CsvDbQSintaxException("un número entero positivo", scan);
			num = scan.getNum();
			if (!Util.isInteger(num, 1, Integer.MAX_VALUE))
				throw new CsvDbQSintaxException("un número entero en el rango [1, " + Integer.MAX_VALUE + "]", scan);
			offset = count;
			count = num.intValue();
			token = scan.nextToken();
		}

		return new LimitExpression(offset, count);
	}

	private OrderByExpressions getOrderBy(SelectExpressions ses) {
		var orderBy = new OrderByExpressions();

		// [ORDER BY]
		if (token != Tokens.ORDER)
			return orderBy;
		token = scan.nextToken();
		match(Tokens.BY, "BY");
		do {
			int index;
			boolean asc;

			// (n | label)
			if (token == Scanner.NUMBER) {
				final Double num = scan.getNum();
				if (!Util.isInteger(num, 1, ses.size())) {
					throw new CsvDbQSintaxException("un número entero en el rango [1, " + ses.size() + "]", scan);
				}
				index = num.intValue() - 1;
			} else if (token == Scanner.IDENT || token == Scanner.STRING) {
				String label;
				if (token == Scanner.IDENT) {
					label = scan.lex();
				} else {
					label = scan.lexString();
				}
				var optIndex = IntStream.range(0, ses.size()).filter(i -> ses.get(i).getLabel().equalsIgnoreCase(label))
						.findFirst();
				if (optIndex.isPresent()) {
					index = optIndex.getAsInt();
				} else {
					throw new CsvDbQSintaxException("un nombre de campo o etiqueta existentes", scan);
				}
			} else {
				throw new CsvDbQSintaxException("número entero, nombre de campo o etiqueta", scan);
			}
			token = scan.nextToken();

			// [ASC | DESC]
			if (token == Tokens.ASC || token == Tokens.DESC) {
				asc = token == Tokens.ASC;
				token = scan.nextToken();
			} else {
				asc = true;
			}
			orderBy.add(new OrderByExpression(index, asc));
		} while (matchIf(Tokens.COMMA));

		return orderBy;
	}
	
	private List<Integer> getGroupBy(SelectExpressions ses) {
		var groupBy = new ArrayList<Integer>();

		// [GROUP BY]
		if (token != Tokens.GROUP)
			return groupBy;
		token = scan.nextToken();
		match(Tokens.BY, "BY");
		do {
			int index;

			// (n | label)
			if (token == Scanner.NUMBER) {
				final Double num = scan.getNum();
				if (!Util.isInteger(num, 1, ses.size())) {
					throw new CsvDbQSintaxException("un número entero en el rango [1, " + ses.size() + "]", scan);
				}
				index = num.intValue() - 1;				
			} else if (token == Scanner.IDENT || token == Scanner.STRING) {
				String label;
				if (token == Scanner.IDENT) {
					label = scan.lex();
				} else {
					label = scan.lexString();
				}
				var optIndex = IntStream.range(0, ses.size()).filter(i -> ses.get(i).getLabel().equalsIgnoreCase(label))
						.findFirst();
				if (optIndex.isPresent()) {
					index = optIndex.getAsInt();
				} else {
					throw new CsvDbQSintaxException("un nombre de campo o etiqueta existentes", scan);
				}
			} else {
				throw new CsvDbQSintaxException("número entero, nombre de campo o etiqueta", scan);
			}
			if (ses.get(index).isAggFunctions()) {
				throw new CsvDbQSintaxException("No puede agruparse por '" +
						ses.get(index).getExpression() +
						"'  si contiene funciones de agregado."); 
			}
			token = scan.nextToken();
			groupBy.add(index);
		} while (matchIf(Tokens.COMMA));

		return groupBy;
	}

	private SelectExpression selectExpression() {
		// expr
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
		} while (token != Tokens.AS && (token != Tokens.COMMA || sumParenth != 0) && token != Tokens.FROM
				&& token != Scanner.EOF);

		// Valores por defecto
		String label = expression;		

		// ["as" label]
		if (token == Tokens.AS) {
			token = scan.nextToken();
			if (token != Scanner.IDENT && token != Scanner.STRING) {
				throw new CsvDbQSintaxException("identificador o string", scan);
			}
			// Etiqueta personalizada
			if (token == Scanner.IDENT) {
				label = scan.lex();
			} else {
				label = scan.lexString();
			}
			token = scan.nextToken();
		}

		return new SelectExpression(expression, label);
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
			if (token == Scanner.IDENT || scan.getTokenClass() == Scanner.KEYWORD) {
				lex = lex.toLowerCase();
			}
			script += lex;
			token = scan.nextToken();
		}
		scan.setTextIn(script);
		token = scan.nextToken();
	}

	private List<AggregateFunction> extractAggFunctions(String selectExpression) {
		List<Integer> aggTokens = List.of(Tokens.COUNT, Tokens.SUM, Tokens.AVG, Tokens.MIN, Tokens.MAX);
		var aggFunctions = new ArrayList<AggregateFunction>();				
		String aggFunction = "";
		int aggFunctionId = -1;
		boolean inAggFunc = false;
		int openParent = 0;
		
		scan.setTextIn(selectExpression);
		token = scan.nextToken();
		do {
			if (aggTokens.contains(token)) {
				if (inAggFunc) {
					throw new CsvDbQSintaxException("No se permiten funciones de agregado anidadas.");
				}
				aggFunction = scan.lex();
				aggFunctionId = token;
				token = scan.nextToken();
				match(Tokens.OPEN_PARENT, "(");
				aggFunction += " (";
				openParent++;
				do {					
					aggFunction += " " + scan.lex();
					if (token == Tokens.OPEN_PARENT) {
						openParent++;
					} else if (token == Tokens.CLOSED_PARENT) {
						openParent--;
					}
					if (openParent == 0) {
						final String af = aggFunction;
						if (!aggFunctions.stream().anyMatch(f -> f.getAggregateExpression().getExpression().equals(af))) {
							aggFunctions.add(new AggregateFunction(aggFunctionId, new AggregateExpression(af,
									af.substring(af.indexOf('(') + 1, af.length() - 1))));
						}
						inAggFunc = false;
					}
					token = scan.nextToken();
				} while (openParent > 0 && token != Scanner.EOF);
			} else {
				token = scan.nextToken();
			}
		} while (token != Scanner.EOF);				
		
		return aggFunctions;
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
		} while (token != Tokens.GROUP &&
				token != Tokens.ORDER &&
				token != Tokens.LIMIT &&
				token != Scanner.EOF);		
		
		return where;
	}
	
	private String getHaving() {
		if (!matchIf(Tokens.HAVING)) return null;		
		String having = "";
		do {
			if (having.length() > 0) {
				having += " ";
			}
			having += scan.lex();
			token = scan.nextToken();
		} while (token != Tokens.ORDER &&
				token != Tokens.LIMIT &&
				token != Scanner.EOF);		
		
		return having;
	}
	
	private String getTableName() {
		if (token != Scanner.IDENT && token != Scanner.STRING) {
			throw new CsvDbQSintaxException("nombre de tabla", scan);
		}
		final String tableName = token == Scanner.IDENT ? scan.lex() : scan.lexString();
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
		} while (!(sumParenth == 0 &&
				(
					token == Tokens.WHERE ||
					token == Tokens.GROUP ||
					token == Tokens.HAVING ||
					token == Tokens.ORDER ||
					token == Tokens.LIMIT ||
					token == Tokens.CLOSED_PARENT
				)) && token != Scanner.EOF);

		return expression;
	}

}
