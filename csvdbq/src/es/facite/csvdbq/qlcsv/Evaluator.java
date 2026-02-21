package es.facite.csvdbq.qlcsv;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import es.facite.csvdbq.core.CsvDecimalFormat;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.security.Crypto;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

public class Evaluator {
	private TextScanner scan;	
	private int token;
	// Resultado de la última expresión evaluada
	private Object result;
	// Tabla de de nombres de variables y sus valores
	private Map<String, Object> vars;
	// Configuración de ficheros CSV
	private CsvConfig config;
	// Para el formateo de número decimales en formato CSV
	private final CsvDecimalFormat csvDecimalFormat;
	// Caché de expresiones regulares para "RELIKE"
	private Map<String, Pattern> reLikeCache;
	// Caché de subconsultas "SELECT"
	private Map<String, ResultSelect> resultSelectCache;
	
	public Evaluator(CsvConfig config) {
		this.config = config;
		csvDecimalFormat = CsvDecimalFormat.of(this.config.isDecimalSeparatorPoint());
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
		reLikeCache = new HashMap<>();
		resultSelectCache = new HashMap<>();
	}
	
	/**
	 * Establece un mapa con los nombres de las variables y sus valores.
	 * @param vars
	 */
	public void setVars(Map<String, Object> vars) {
		this.vars = vars;
	}

	/**
	 * Devuelve el resultado de la evaluación de una expresión para una serie de
	 * variables.
	 * 
	 * @param expr	 
	 * @return
	 */
	public Object eval(String expr) {		
		scan.setTextIn(expr);		
		token = scan.nextToken();
		expresion();
		match(Scanner.EOF, "fin de la expresión");

		return result;
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
	
	private CsvDbQException incompatibleOperands(Object on1, Object on2, String operation) {
		final String type1 = on1 != null ? on1.getClass().getSimpleName() : "null";
		final String type2 = on2 != null ? on2.getClass().getSimpleName() : "null";
		
		return new CsvDbQException(type1 + " " + operation + " " + type2 + ": operación no admitida.");
	}
	
	private CsvDbQException incompatibleOperand(Object on, String operation) {
		final String type = on != null ? on.getClass().getSimpleName() : "null";
		
		return new CsvDbQException(operation + " " + type + ": operación no admitida.");
	}
			
	private void expresion() {
		// expresion -> opOr
		opOr();
	}

	private void opOr() {
		// opOr -> opAnd restoOpOr
		opAnd();
		restoOpOr();
	}

	private void restoOpOr() {
		// restoOpOr -> {("OR" | "RELIKE" | "BETWEEN" | "IN") opAnd}
		while (token == Tokens.OR ||
				token == Tokens.RELIKE ||
				token == Tokens.BETWEEN ||
				token == Tokens.IN) {									
			int op = token;
			Object res = result;
			
			token = scan.nextToken();			
			if (op == Tokens.BETWEEN) {
				opNot();
				Object limInf = result;
				match(Tokens.AND, "AND");
				opNot();
				between(res, limInf, result);
				restoOpAnd();
			} else if (op == Tokens.IN) {
				match(Tokens.OPEN_PARENT, "(");
				Boolean match = null;
				do {					
					expresion();
					if (match == null || !match) {
						if (result instanceof CsvRows rows) {
							if (rows.size() > 0) {
								for (var row : rows) {
									result = eq(res, row.get(0));
									if (result instanceof Boolean bln && bln) break;								
								}
							} else {
								result = false;
							}
						} else {
							result = eq(res, result);
						}
						match = result != null ? (Boolean) result : null;				
					}
				} while (matchIf(Tokens.COMMA));
				match(Tokens.CLOSED_PARENT, ")");
				result = match;
			} else {
				opAnd();
				performOpOr(op, res);				
			}						
		}
	}
	
	private void opAnd() {
		// opAnd -> opNot restoOpAnd
		opNot();
		restoOpAnd();
	}
		
	private void restoOpAnd() {
		// restoOpAnd -> {"AND" opNot}
		while (token == Tokens.AND) {
			Object res = result;
			
			token = scan.nextToken();
			opNot();			
			and(res, result);			
		}
	}
	
	private void opNot() {
		// opNot -> {not | !} opRel
		if (token == Tokens.NOT) {			
			token = scan.nextToken();
			opNot();
			performOpUn(Tokens.NOT);		
		} else {
			opRel();
		}		
	}

	private void opRel() {
		// opRel -> sumaResta restoOpRel
		sumaResta();
		restoOpRel();
	}

	private void restoOpRel() {
		// restoOpRel -> {(">" | "<" | ">=" | "<=" | "=" | "<>" | "==" | "!=") sumaResta}
		while (token == Tokens.GT || token == Tokens.LT
			|| token == Tokens.GE || token == Tokens.LE
			|| token == Tokens.EQ || token == Tokens.ASIG
			|| token == Tokens.NE || token == Tokens.IS) {
			int op = token;
			Object res = result;
			
			token = scan.nextToken();			
			if (op != Tokens.IS) {
				sumaResta();
				performOpRel(op, res);
			} else {
				boolean negate;
				if (token == Tokens.NOT) {
					token = scan.nextToken();
					negate = true;
				} else {
					negate = false;
				}
				match(Tokens.NULL, "NULL");
				result = negate ? result != null : result == null;
			}
		}
	}

	private void sumaResta() {
		// sumaResta -> mulDiv restoSumaResta
		mulDiv();
		restoSumaResta();
	}

	private void restoSumaResta() {
		// restoSumaResta -> {("+" | "-") mulDiv}
		while (token == Tokens.ADD || token == Tokens.SUB) {
			int op = token;
			Object res = result;
			token = scan.nextToken();
			mulDiv();
			performOpBin(op, res);
		}
	}

	private void mulDiv() {
		// mulDiv -> opUn restoMulDiv
		opUn(false);
		restoMulDiv();
	}

	private void restoMulDiv() {
		// restoMulDiv -> {("*"|"/"|"%") opUn}
		while (token == Tokens.MUL || token == Tokens.DIV || token == Tokens.MOD) {
			Object res = result;
			int op = token;
			token = scan.nextToken();
			opUn(false);
			performOpBin(op, res);
		}
	}

	private void opUn(boolean callOpPrim) {
		// opUn -> {("+" | "-") opUn} | opPrim | opPow
		if (token == Tokens.ADD || token == Tokens.SUB) {
			int op = token;
			token = scan.nextToken();
			opUn(callOpPrim);
			performOpUn(op);
		} else if (callOpPrim) {
			opPrim();
		} else {
			opPow();
		}
	}	

	private void opPow() {
		// opPow -> opPrim restoOpPow
		opPrim();
		restoOpPow();
	}

	private void restoOpPow() {
		// restoOpPow -> {"**" opUn}
		while (token == Tokens.POW) {
			Object res = result;
			token = scan.nextToken();
			opUn(true);			
			result = pow(res, result);
		}
	}	

	private void opPrim() {
		// opPrim -> (expresión) | num | string | id |
		//	callFunction(p1, ...)

		switch (token) {
		case Tokens.OPEN_PARENT:
			token = scan.nextToken();
			expresion();
			match(Tokens.CLOSED_PARENT, ")");
			break;		
		case Scanner.NUMBER:			
			result = scan.getNum();			
			token = scan.nextToken();
			break;
		case Scanner.STRING:
			result = scan.lexString();
			token = scan.nextToken();
			break;
		case Tokens.TRUE:
		case Tokens.FALSE:
			result = token == Tokens.TRUE;
			token = scan.nextToken();
			break;
		case Tokens.NULL:
			result = null;
			token = scan.nextToken();
			break;		
		case Scanner.IDENT:
			result = vars.get(scan.lex());
			if (result == null && !vars.containsKey(scan.lex())) {
				throw new CsvDbQSintaxException("un nombre de campo existente", scan);
			}
			token = scan.nextToken();
			break;
		case Tokens.SELECT:			
			var select = selectExpression();													
			var resultSelect = resultSelectCache.get(select);
			if (resultSelect == null) {
				resultSelect = new Select(config).execute(select);
				if (resultSelect.getHeaders().size() > 1) {
					throw new CsvDbQException("Una subconsulta SELECT ha devuelto "
							+ "más de una columna.");
				}
				resultSelectCache.put(select, resultSelect);						
			}
			if (resultSelect.getRows().size() == 1) {
				result = resultSelect.getRows().get(0).get(0);
			} else {
				result = resultSelect.getRows();
			}
			break;
		case Tokens.STRING:
			string();
			break;
		case Tokens.SUBSTRING:
			substring();
			break;
		case Tokens.SHA:
			sha();
			break;
		case Tokens.IIF:
			iif();
			break;		
		case Tokens.TYPEOF:
			typeof();
			break;
		default:
			throw new CsvDbQSintaxException("expresión", scan);			
		}
	}
	
	private void not() {		
		if (result instanceof Boolean bln) {
			result = !bln;			
		} else if (result != null) {
			throw incompatibleOperand(result, "NOT");
		}
	}
	
	private void and(Object on1, Object on2) {				
		if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			result = bln1 && bln2;	
		} else if (on1 instanceof Boolean bln1 && on2 == null) {
			result = bln1 ? null : false;
		} else if (on1 == null && on2 instanceof Boolean bln2) {
			result = bln2 ? null : false;
		} else {
			throw incompatibleOperands(on1, on2, "AND");
		}
	}
	
	private void between(Object expr, Object limInf, Object limSup) {		
		if (expr instanceof Double e &&
				limInf instanceof Double li &&
				limSup instanceof Double ls) {
			result = e >= li && e <= ls;
		} else if (expr instanceof String e &&
					limInf instanceof String li &&
					limSup instanceof String ls) {
			result = e.compareTo(li) >= 0 && e.compareTo(ls) <= 0;
		} else if (expr instanceof Boolean e &&
				limInf instanceof Boolean li &&
				limSup instanceof Boolean ls) {
			result = e.compareTo(li) >= 0 && e.compareTo(ls) <= 0;	
		} else	if (expr == null || limInf == null || limSup == null) {
			result = null;
		} else {
			throw new CsvDbQException("Operación no admitida: " +
					expr + " BETWEEN " + limInf + " AND " + limSup);
		}
	}
	
	private Object or(Object on1, Object on2) {
		if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1 || bln2;		
		} else if (on1 instanceof Boolean bln1 && on2 == null) {
			return bln1 ? true : null;
		} else if (on1 == null && on2 instanceof Boolean bln2) {
			return bln2 ? true : null;
		} else {
			throw incompatibleOperands(on1, on2, "OR");
		}
	}		

	private Object lt(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1.compareTo(dbl2) < 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) < 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) < 0;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "<");
		}
	}

	private Object le(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1.compareTo(dbl2) <= 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) <= 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) <= 0;	
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "<=");
		}				
	}

	private Object eq(Object on1, Object on2) {		
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1.compareTo(dbl2) == 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) == 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) == 0;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			return false;
		}
	}

	private Object gt(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1.compareTo(dbl2) > 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) > 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) > 0;	
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, ">");
		}		
	}

	private Object ge(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1.compareTo(dbl2) >= 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) >= 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) >= 0;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, ">=");
		}		
	}

	private Object ne(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {			
			return dbl1.compareTo(dbl2) != 0;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1.compareToIgnoreCase(str2) != 0;
		} else if (on1 instanceof Boolean bln1 && on2 instanceof Boolean bln2) {
			return bln1.compareTo(bln2) != 0;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			return true;
		}
	}	
	
	private Object reLike(Object on1, Object on2) {
		if (on1 instanceof String str1 && on2 instanceof String str2) {
			var pattern = reLikeCache.get(str2);
			
			if (pattern == null) {
				pattern = Pattern.compile(str2, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
				reLikeCache.put(str2, pattern);
			}
			
			return pattern.matcher(str1).find();
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "LIKE");
		}
	}

	private Object add(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1 + dbl2;
		} else if (on1 instanceof String str1 && on2 instanceof String str2) {
			return str1 + str2;
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "+");
		}		
	}

	private Object sub(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1 - dbl2;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "-");
		}				
	}

	private Object mul(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1 * dbl2;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "*");
		}		
	}

	private Object div(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1 / dbl2;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "/");
		}		
	}

	private Object mod(Object on1, Object on2) {		
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return dbl1 % dbl2;		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "%");
		}		
	}

	private Object pow(Object on1, Object on2) {
		if (on1 instanceof Double dbl1 && on2 instanceof Double dbl2) {
			return Math.pow(dbl1, dbl2);		
		} else if (on1 == null || on2 == null) {
			return null;
		} else {
			throw incompatibleOperands(on1, on2, "**");
		}		
	}

	private void addUnary() {
		if (result == null) return;
		
		if (!(result instanceof Double)) {
			throw incompatibleOperand(result, "+");
		}		
	}

	private void subUnary() {	
		if (result == null) return;
		
		if (result instanceof Double dbl) {
			result = -dbl;					
		} else {
			throw incompatibleOperand(result, "-");
		}				
	}
	
	private void performOpOr(int op, Object on) {		
		switch (op) {
		case Tokens.OR:
			result = or(on, result);
			break;
		case Tokens.RELIKE:
			result = reLike(on, result);
			break;		
		}
	}

	/**
	 * Realiza una operación entre entre el operador pasado y el resultado actual,
	 * asignándolo como nuevo resultado.
	 * 
	 * @param op
	 * @param on
	 * @return
	 */
	private void performOpRel(int op, Object on) {		
		switch (op) {
		case Tokens.LT:
			result = lt(on, result);
			break;
		case Tokens.GT:
			result = gt(on, result);
			break;
		case Tokens.LE:
			result = le(on, result);
			break;
		case Tokens.GE:
			result = ge(on, result);
			break;
		case Tokens.EQ:
		case Tokens.ASIG:
			result = eq(on, result);
			break;
		case Tokens.NE:
			result = ne(on, result);
			break;		
		}
	}

	/**
	 * Realiza una operación entre entre el operador pasado y el resultado actual,
	 * asignándolo como nuevo resultado.
	 * 
	 * @param op
	 * @param on
	 * @return
	 */
	private void performOpBin(int op, Object on) {		
		switch (op) {
		case Tokens.ADD:
			result = add(on, result);
			break;
		case Tokens.SUB:
			result = sub(on, result);
			break;
		case Tokens.MUL:
			result = mul(on, result);
			break;
		case Tokens.DIV:
			result = div(on, result);
			break;
		case Tokens.MOD:
			result = mod(on, result);
			break;
		case Tokens.POW:
			result = pow(on, result);
			break;
		}
	}

	/**
	 * Realiza una operación unaria con el resultado y se la asigna al mismo.
	 * 
	 * @param op
	 */
	private void performOpUn(int op) {		
		switch (op) {
		case Tokens.ADD:
			addUnary();
			break;
		case Tokens.SUB:
			subUnary();
			break;
		case Tokens.NOT:
			not();
			break;
		}
	}
	
	/**
	 * Devuelve la representación de cadena del argumento. Si el argumento es <null>
	 * devuelve <null>.
	 * string(any)
	 */
	private void string() {		
		token = scan.nextToken();
		match(Tokens.OPEN_PARENT, "(");
		expresion();		
		match(Tokens.CLOSED_PARENT, ")");
		if (result instanceof Double dbl) {
			result = csvDecimalFormat.format(dbl);
		} else {
			result = result != null ? result.toString() : null;
		}
	}

	/**
	 * Dada una cadena, devuelve una subcadena desde una posición inicial incluida, hasta
	 * una posición final (opcional), no incluida. Si algún argumento es null, devuelve null:
	 * substring(str, start[, end]).
	 */
	private void substring() {
		token = scan.nextToken();
		match(Tokens.OPEN_PARENT, "(");
		expresion();		
		String str = (String) result;
		match(Tokens.COMMA, ",");
		expresion();		
		Integer start = result != null ? ((Double) result).intValue() : null;
		Integer end = null;
		if (token == Tokens.COMMA) {
			token = scan.nextToken();
			expresion();			
			end = result != null ?((Double) result).intValue() : null;
		} else {
			end = str != null ? str.length() : null;
		}
		match(Tokens.CLOSED_PARENT, ")");
		if (str != null && start != null && end != null) {
			result = str.substring(start, end);
		} else {
			result = null;
		}
	}

	/**
	 * Devuelve el hash SHA-256 (SHA2) de la cadena de entrada. Si el argumento es
	 * <null> devuelve <null>:
	 * sha(string)
	 */
	private void sha() {		
		token = scan.nextToken();
		match(Tokens.OPEN_PARENT, "(");
		expresion();
		match(Tokens.CLOSED_PARENT, ")");
		if (result != null) {
			result = Crypto.sha256((String) result, false);
		}
	}	
	
	/**
	 * Evalua una condición y devuelve la primera expresión si es verdadera, o la
	 * segunda si es falsa. Si la condición es <null> devuelve <null>. 
	 */
	private void iif() {
		token = scan.nextToken();
		match(Tokens.OPEN_PARENT, "(");
		expresion();		
		Boolean cond = (Boolean) result;
		match(Tokens.COMMA, ",");
		expresion();
		Object ifTrue = result;
		match(Tokens.COMMA, ",");
		expresion();
		
		if (cond != null) {
			if (cond) {
				result = ifTrue; 
			} // Si no, la última expresión ya asigno a <result> el valor devuelto
		} else {
			result = null;
		}
		match(Tokens.CLOSED_PARENT, ")");		
	}	
	
	/**
	 * Devuelve una cadena con el tipo del argumento. Si el argumento es <null>
	 * devuelve <null>:
	 * typeof(any):
	 * 		null 	=> null
	 * 		String	=> 'string'
	 * 		Double	=> 'number'
	 * 		Boolean 	=> 'boolean'	
	 */
	private void typeof() {		
		token = scan.nextToken();
		match(Tokens.OPEN_PARENT, "(");
		expresion();
		if (result instanceof Double) {
			result = "number";
		} else if (result instanceof String) {
			result = "string";
		} else if (result instanceof Boolean) {
			result = "boolean";
		} else {
			result = null;
		}
		match(Tokens.CLOSED_PARENT, ")");
	}
	
	private String selectExpression() {
		// "SELECT" ... ("," | ")")		
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
		} while (!(sumParenth == 0 && (token == Tokens.COMMA || token == Tokens.CLOSED_PARENT)) &&				
				token != Scanner.EOF);

		return expression;
	}
}
