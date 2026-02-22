package es.facite.csvdbq.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.ResultSelect;
import es.facite.text.CsvScanner;

/**
 * Devuelve información de las cabeceras de una tabla en CSV.
 */
public class CsvHeaders implements Iterable<String>, AutoCloseable {	
	private final CsvIterator csvIterator;
	private final Iterator<CsvRow> iterator;
	private CsvRow headerRow;
	private List<String> headers;
	private List<String> headersNormalize;
	
	public CsvHeaders(CsvScanner scan, CsvConfig config) {
		try {
			csvIterator = new CsvIterator(scan, config);			
			iterator = csvIterator.iterator();		
			if (!iterator.hasNext()) {
				throw new CsvDbQException("Las cabeceras son obligatorias.");
			}
			initHeaders(iterator.next());									
		} catch (Exception e) {
			close();
			throw new CsvDbQException(e);
		}
	}
	
	public CsvHeaders(ResultSelect resultSelect) {
		try {
			csvIterator = null;			
			iterator = resultSelect.getRows().iterator();
			initHeaders(resultSelect.getHeaders());									
		} catch (Exception e) {
			close();
			throw new CsvDbQException(e);
		}
	}
	
	private void initHeaders(CsvRow headersRow) {
		this.headerRow = headersRow;		
		headers = new ArrayList<>();
		headersNormalize = new ArrayList<>();
		
		for (int i = 0; i < headerRow.size(); i++) {
			String header = headerRow.get(i).toString();			
			headers.add(header);
			// Normaliza la cabecera
			if (!CsvScanner.isIdent(header)) {
				header = "[" + header + "]";
			}
			headersNormalize.add(header.toLowerCase());
		}
	}
	
	public List<String> getHeaders() {
		return headers;
	}
	
	public List<String> getHeadersNormalize() {
		return headersNormalize;
	}
	
	public int size() {
		return headers.size();
	}
	
	public String get(int index) {
		return headers.get(index);
	}
	
	public String getLowerCase(int index) {
		return headersNormalize.get(index);
	}
		
	public Iterator<CsvRow> getIterator() {
		return iterator;
	}
	
	@Override
	public String toString() {
		return headerRow.toString();
	}
	
	public String toEncrypted() {
		return headerRow.toEncrypted();
	}

	@Override
	public Iterator<String> iterator() {
		return headers.iterator();
	}

	@Override
	public void close() {
		if (csvIterator != null) {
			csvIterator.close();
		}
	}
}
