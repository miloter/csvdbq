package es.facite.csvdbq.exception;

public class CsvDbQException extends RuntimeException {	
	private static final long serialVersionUID = 8947247189302736912L;
	
	public CsvDbQException(String msg) {
		super(msg);
	}
	
	public CsvDbQException(Throwable cause) {
		super(cause);
	}
}
