package es.facite.csvdbq.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.qlcsv.CsvConfig;

public class CsvRow implements Iterable<Object> {
	private final List<Object> row;
	private final CsvConfig config;
	private final String sQuotes;
	private final String doubleQuotes;
	private final CsvDecimalFormat csvDecimalFormat;
	
	public CsvRow() {
		this(new CsvConfig(TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP, TokensCsv.DEFAULT_DS_POINT));
	}	
		
	public CsvRow(CsvConfig config) {
		this.row = new ArrayList<>();
		this.config = config;
		sQuotes = "" + this.config.getQuotes();
		doubleQuotes = sQuotes + sQuotes;		
		csvDecimalFormat = CsvDecimalFormat.of(config.isDecimalSeparatorPoint());				
	}
	
	public void add(Object value) {
		row.add(value);
	}
	
	public int size() {
		return row.size();
	}	
	
	public Object get(int index) {
		return row.get(index);
	}
	
	public void set(int index, Object value) {
		row.set(index, value);
	}
	
	public List<Object> values() {
		return row;
	}
	
	@Override
	public String toString() {
		var sbRow = new StringBuilder();
		
		for (var value : row) {
			var sValue = toString(value);
			
			if (sbRow.length() > 0) {
				sbRow.append(config.getSep());
			} 
			boolean hasSep = sValue.indexOf(config.getSep()) >= 0;
			boolean hasQuotes = sValue.indexOf(config.getQuotes()) >= 0;
			boolean hasLines = sValue.indexOf('\r') >= 0 || sValue.indexOf('\n') >= 0;

			if (hasSep || hasQuotes || hasLines)
				sbRow.append(config.getQuotes());
			sbRow.append(sValue);
			if (hasSep || hasQuotes || hasLines)
				sbRow.append(config.getQuotes());
		}
			
		return sbRow.toString();
	}
	
	public String toEncrypted() {
		var sbRow = new StringBuilder();
		
		for (var value : row) {
			var sValue = config.getCipher().encrypt(toString(value));
			
			if (sbRow.length() > 0) {
				sbRow.append(config.getSep());
			} 

			sbRow.append(sValue);
		}
			
		return sbRow.toString();
	}		
	
	public String toString(int index) {
		return toString(row.get(index));
	}
	
	public String toString(Object value) {
		if (value == null) return "null";
		
		if (value instanceof String str) {			
			return str.replace(sQuotes, doubleQuotes);			
		}
		if (value instanceof Boolean bln) {
			 return bln ? "true" : "false";
		}
		if (value instanceof Double dbl) {
			return csvDecimalFormat.format(dbl);
		}		
		throw new CsvDbQException("Tipo de dato " +
				value.getClass().getTypeName() + " no implementado.");		
	}	

	@Override
	public Iterator<Object> iterator() {
		return row.iterator();
	}
	
	public static CsvRow create(CsvConfig config, Object... values) {
		var row = new CsvRow(config);
		for (var value : values) {
			row.add(value);
		}
		
		return row;
	}
}
