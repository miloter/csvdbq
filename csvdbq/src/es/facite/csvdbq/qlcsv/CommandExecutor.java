package es.facite.csvdbq.qlcsv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.exception.CsvDbQSintaxException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.security.Crypto;
import es.facite.text.Scanner;
import es.facite.text.TextScanner;

/**
 * Procesador de comandos de QlCsv.
 */
public class CommandExecutor {
	private TextScanner scan;
	private int token;
	
	private class ExtendFilenameFilter implements FilenameFilter {
		private String filePattern;

		public ExtendFilenameFilter(String filePattern) {
			this.filePattern = filePattern;
		}

		@Override
		public boolean accept(File f, String name) {
			return filePattern.isEmpty() || Pattern.matches(filePattern, name);
		}
	}

	public CommandExecutor() {
		scan = new TextScanner("", true);
		Tokens.initialize(scan);
	}

	/**
	 * 
	 * @param command
	 * @param config
	 * @return
	 * @throws IOException 
	 */
	public String execute(String command, CsvConfig config) {
		ResultSelect resultSelect;
		String result;
		String temp;
		
		scan.setTextIn(command);
		token = scan.nextToken();
		
		switch (token) {
		case Tokens.SELECT:
			resultSelect = new Select(config).execute(command);
			result = resultSelect.toString();
			result += System.lineSeparator() + resultSelect.size() + " filas devueltas.";
			break;
		case Tokens.UPDATE:
			var rowsUpdated = new Update(config).execute(command);
			result = rowsUpdated + " filas actualizdas.";
			break;
		case Tokens.INSERT:
			var rowsInserted = new Insert(config).execute(command);
			result = rowsInserted + " filas insertadas.";
			break;
		case Tokens.DELETE:
			var rowsDeleted = new Delete(config).execute(command);
			result = rowsDeleted + " filas eliminadas.";
			break;
		case Tokens.JOIN:
			resultSelect = new Join(config).execute(command);
			result = resultSelect.toString();
			result += System.lineSeparator() + resultSelect.size() + " filas devueltas.";
			break;
		case Tokens.UNION:
			resultSelect = new Union(config).execute(command);
			result = resultSelect.toString();
			result += System.lineSeparator() + resultSelect.size() + " filas devueltas.";
			break;
		case Tokens.SHOW:
			token = scan.nextToken();
			temp = scan.lex().toLowerCase();
			if (temp.equals("config")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				result = getConfig(config);
			} else if (temp.equals("dir")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				result = config.getDbDir();
			} else if (temp.equals("quotes")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				result = String.valueOf(config.getQuotes());
			} else if (temp.equals("sep")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				temp = String.valueOf(config.getSep());
				if (temp.equals("\t")) {
					temp = "t";
				}
				result = temp;
			} else if (temp.equals("decsep")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				result = config.isDecimalSeparatorPoint() ? "." : ",";
			} else if (temp.equals("headers")) {
				token = scan.nextToken();
				match(Tokens.FROM, "FROM");
				temp = getIdentOrString();
				match(Scanner.EOF, "fin del comando");
				result = "";
				var tempConfig = new CsvConfig(config);
				for (var sep : TokensCsv.CSV_SEP_VALID) {
					tempConfig.setSep(sep);
					result += "Cabeceras para el separador '" + (sep != '\t' ? sep : "t") + "': ";
					try (var headers = new CsvFileHeaders(temp, tempConfig)) {						
						result += headers.size() + System.lineSeparator();
						for (var header : headers) {
							result += "\t" + header + System.lineSeparator();						
						}
					}
				}
			} else if (temp.equals("tables")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				// Calcula la expresión regular del patrón
				var filePattern = "^.+\\.csv$";
				FilenameFilter filter = new ExtendFilenameFilter(filePattern);
				File pathDir = new File(config.getDbDir());
				List<String> list = new ArrayList<>();
				for (File f : pathDir.listFiles(filter)) {
					if (f.isFile()) {
						list.add(f.getName());					
					}
				}				
				result = String.join(System.lineSeparator(),  list) + System.lineSeparator();
				result += System.lineSeparator() + list.size() + " tablas encontradas";
			} else if (temp.equals("cipher")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");									
				result = "Cifrado activado: " + (config.getCipher().isEncrypted() ? "Sí" : "No");
			} else {
				throw unknownCommand(command);
			}
			break;
		case Tokens.SET:
			token = scan.nextToken();
			temp = scan.lex().toLowerCase();
			if (temp.equals("dir")) {
				token = scan.nextToken();
				match(Tokens.ASIG, "=");
				config.setDbDir(getExpression());
				result = "Nuevo directorio de trabajo: " + config.getDbDir();
			} else if (temp.equals("quotes")) {
				token = scan.nextToken();
				match(Tokens.ASIG, "=");
				temp = getExpression();
				if (temp.length() != 1) {
					throw new CsvDbQSintaxException("'" + temp +
							"': se requiere un único caracter de entrecomillado.");
				}
				config.setQuotes(temp.charAt(0));
				result = "Nuevo caracter de entrecomillado: " + config.getQuotes();
			} else if (temp.equals("sep")) {
				token = scan.nextToken();
				match(Tokens.ASIG, "=");
				temp = getExpression();
				if (temp.length() != 1) {
					throw new CsvDbQSintaxException("'" + temp +
							"': se requiere un único caracter de separación de campos.");
				}
				if (temp.equals("t")) {
					temp = "\t";
				}
				config.setSep(temp.isEmpty() ? ' ' : temp.charAt(0));
				result = "Nuevo caracter de separación de campos: " +
						(config.getSep() != '\t' ? config.getSep() : 't');
			} else if (temp.equals("decsep")) {
				token = scan.nextToken();
				match(Tokens.ASIG, "=");
				temp = getExpression();
				if (temp.length() != 1) {
					throw new CsvDbQSintaxException("'" + temp +
							"': se requiere un único caracter de separación decimal.");
				}
				char decSep = temp.charAt(0);				
				if (!(decSep == '.' || decSep == ',')) {
					throw new CsvDbQSintaxException("'" + decSep + "' no es un separador decimal "
							+ "válído, use solo uno de: . (punto), o , (coma).");
				}
				config.setDecimalSeparatorPoint(decSep == '.');
				result = "Nuevo caracter de separación decimal: " +
						(config.isDecimalSeparatorPoint() ? '.' : ',');
			} else if (temp.equals("cipher")) {
				token = scan.nextToken();
				match(Scanner.EOF, "fin del comando");
				result = Crypto.showPasswordDialog("Solicitud de contraseña", "", false);
				if (result != null) {
					config.getCipher().setPassword(result);						
					if (result.isEmpty()) {
						result = "Cifrado activado: No";
					} else {						
						result = "Cifrado activado: Sí";								
					}
				} else {
					result = "Operación cancelada";
				}				
			} else {
				throw unknownCommand(command);
			}
			break;
		case Tokens.CREATE:
			token = scan.nextToken();
			if (token == Tokens.TABLE) {
				token = scan.nextToken();
				var tableName = getIdentOrString();				
				CsvRow headers = null;
				CsvRows rows = new CsvRows();				
				
				if (token == Tokens.OPEN_PARENT) {
					token = scan.nextToken();
					headers = new CsvRow(config);
					do {
						headers.add(getIdentOrString());
					} while (matchIf(Tokens.COMMA));
					match(Tokens.CLOSED_PARENT, ")");
					match(Scanner.EOF, "fin del comando");
				} else {
					// "FROM" "SELECT" ... 
					match(Tokens.FROM, "FROM");				 
					temp = getExpression();													
					resultSelect = new Select(config).execute(temp);					
					headers = resultSelect.getHeaders();
					rows = resultSelect.getRows();
				}				
				// Se intenta crear una tabla con la información suministrada									
				try (BufferedWriter bw = Files.newBufferedWriter(
						Path.of(config.composePath(tableName)), StandardCharsets.UTF_8,
						StandardOpenOption.CREATE_NEW)) {
					bw.write("\ufeff"); // BOM de UTF-8 para Office de Microsoft
					
					// Escribe la fila de cabeceras			
					bw.write(config.getCipher().isEncrypted() ? headers.toEncrypted() : headers.toString());
					// Y el resto de filas
					for (var row : rows) {
						bw.write(TokensCsv.NL);
						bw.write(config.getCipher().isEncrypted() ? row.toEncrypted() : row.toString());								
					}
					
					result = "Tabla " + tableName + "(" + String.join(", ",
							headers.values().stream().map(o -> "'" + o + "'").toList()) +
							") creada con exito.";
				} catch (IOException e) {
					throw new CsvDbQException(e);
				}										
			} else {
				throw unknownCommand(command);
			}
			break;
		case Tokens.DROP:
			token = scan.nextToken();
			if (token == Tokens.TABLE) {
				token = scan.nextToken();
				var tableName = getIdentOrString();
				match(Scanner.EOF, "fin del comando");
				var pathCsv = Path.of(config.composePath(tableName));			
				if (!Files.exists(pathCsv)) {
					throw new CsvDbQException("El fichero '" + pathCsv + "' no existe.");
				} else if (!Files.isRegularFile(pathCsv)) {
					throw new CsvDbQException("'" + pathCsv + "' no es un fichero CSV.");
				} else {
					try {
						Files.delete(pathCsv);
						result = "Fichero '" + pathCsv + "' eliminado con exito."; 
					} catch (IOException e) {
						throw new CsvDbQException(e);
					}
				}
			} else {
				throw unknownCommand(command);
			}
			break;
		case Tokens.ENCRYPT:
		case Tokens.DECRYPT:
			final int tokenCrypt = token;
			
			token = scan.nextToken();
			if (token == Tokens.TABLE) {
				token = scan.nextToken();
				
				var encryptedTable = new EncryptedTable(config);
				result = "";
				do {
					var tableName = getIdentOrString();	
					encryptedTable.execute(tableName, tokenCrypt == Tokens.ENCRYPT ? 
							EncryptedTable.ENCRYPT_MODE : EncryptedTable.DECRYPT_MODE);
					result += "Tabla '" + tableName + "' " + (tokenCrypt == Tokens.ENCRYPT ?
							"encriptada" : "desencriptada") + " con éxito." +
							System.lineSeparator();
				} while (matchIf(Tokens.COMMA));
				match(Scanner.EOF, "fin del comando");
			} else {
				throw unknownCommand(command);
			}
			break;
		default:			
			throw unknownCommand(command);			
		}
		
		return result;
	}
	
	private CsvDbQSintaxException unknownCommand(String command) {
		return new CsvDbQSintaxException("'" + command + "' no se reconoce como un "
					+ "comando, por favor, revise su sintaxis.");
	}

	private void match(int expected, String str) {
		if (token == expected) {
			token = scan.nextToken();
		} else {
			throw new CsvDbQSintaxException(str, scan);
		}
	}
	
	private String getConfig(CsvConfig config) {
		return "********************** Configuración de ficheros CSV **********************" +
				System.lineSeparator() +
				"Directorio de trabajo: " + config.getDbDir() + System.lineSeparator() +
				"Caracter de entrecomillado: " + config.getQuotes() + System.lineSeparator() +
				"Caracter de separación de campos: " + config.getSep() + System.lineSeparator() +
				"Caracter de separación decimal: " +
				(config.isDecimalSeparatorPoint() ? '.' : ',') + System.lineSeparator() +
				"Cifrado activado: " + (config.getCipher().isEncrypted() ? "Sí" : "No") + System.lineSeparator() +
				"***************************************************************************";
	}
	
	private String getExpression() {
		var sb = new StringBuilder();

		scan.setIgnoreSpacesInLine(false);
		while (token != Scanner.EOF) {
			sb.append(scan.lex());
			token = scan.nextToken();
		}
		scan.setIgnoreSpacesInLine(true);
		
		return sb.toString().trim();
	}
	
	private String getIdentOrString() {
		if (token != Scanner.IDENT && token != Scanner.STRING) {
			throw new CsvDbQSintaxException("nombre de tabla", scan);
		}
		final String tableName = token == Scanner.IDENT ? scan.lex() : scan.lexString();
		token = scan.nextToken();
		
		return tableName;			
	}
	
	private boolean matchIf(int expected) {
		if (token == expected) {
			token = scan.nextToken();
			return true;
		} else {
			return false;
		}
	}
}
