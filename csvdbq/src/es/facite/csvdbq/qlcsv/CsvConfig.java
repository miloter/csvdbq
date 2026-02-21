package es.facite.csvdbq.qlcsv;

import java.nio.file.Files;
import java.nio.file.Path;

import es.facite.csvdbq.core.AppInfo;
import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;

public class CsvConfig {
	public static final String TABLE_SUFFIX = ".csv";
	
	private String dbDir;
	private char quotes;
	private char sep;	
	private boolean decimalSeparatorPoint;
	private CsvConfigCipher cipher;
	
	public CsvConfig() {
		this(AppInfo.USER_DIR);
	}
	
	public CsvConfig(String dbDir) {
		this(dbDir, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP, TokensCsv.DEFAULT_DS_POINT);
	}
	
	public CsvConfig(char quotes, char sep, boolean decimalSeparatorPoint) {
		this(AppInfo.USER_DIR, quotes, sep, decimalSeparatorPoint);
	}
	
	public CsvConfig(CsvConfig config) {
		this(config.getDbDir(), config.getQuotes(), config.getSep(), config.isDecimalSeparatorPoint());
		cipher.setPassword(config.getCipher().getPassword());
		cipher.setReadPlainText(config.getCipher().isReadPlainText());
	}
	
	public CsvConfig(String dbDir, char quotes, char sep, boolean decimalSeparatorPoint) {
		setDbDir(dbDir);
		setQuotes(quotes);
		setSep(sep);
		setDecimalSeparatorPoint(decimalSeparatorPoint);
		cipher = new CsvConfigCipher();
	}
	
	public String composePath(String tableName) {
		return Path.of(dbDir, tableName) + TABLE_SUFFIX;
	}
	
	public String getDbDir() {
		return dbDir;
	}
	
	public void setDbDir(String dbDir) {
		var p = Path.of(dbDir);
		
		if (!(Files.exists(p) && Files.isDirectory(p))) {
			throw new CsvDbQException("La ruta '" + dbDir + "' no existe o no es un directorio.");
		}
		
		this.dbDir = p.normalize().toAbsolutePath().toString();
	}
	
	public char getQuotes() {
		return quotes;
	}
	
	public void setQuotes(char quotes) {
		TokensCsv.checkQuotes(quotes);
		this.quotes = quotes;
	}

	public char getSep() {
		return sep;
	}
	
	public void setSep(char sep) {
		TokensCsv.checkSep(sep);
		this.sep = sep;
	}

	public boolean isDecimalSeparatorPoint() {
		return decimalSeparatorPoint;
	}
	
	public void setDecimalSeparatorPoint(boolean decimalSeparatorPoint) {
		this.decimalSeparatorPoint = decimalSeparatorPoint;
	}
	
	public CsvConfigCipher getCipher() {
		return cipher;
	}
	
	@Override
	public String toString() {
		return dbDir;
	}	
}
