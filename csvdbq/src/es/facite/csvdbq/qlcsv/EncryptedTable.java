package es.facite.csvdbq.qlcsv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.iterator.CsvFileHeaders;
import es.facite.csvdbq.util.Util;

/**
 * Encripta o desencripta una tabla.
 */
public class EncryptedTable {
	public static final int ENCRYPT_MODE = 0;
	public static final int DECRYPT_MODE = 1;
	
	private CsvConfig config;

	public EncryptedTable(CsvConfig config) {
		this.config = config;		
	}

	/**
	 * Ejecuta el cifrado o descrifrado de una tabla.
	 * 
	 * @param tableName
	 * @param mode El modo de operación: EncryptedTable.ENCRYPT_MODE para encriptar o
	 * EncryptedTable.DECRYPT_MODE para desencriptar.
	 */
	public void execute(String tableName, int mode) {
		if (!config.getCipher().isEncrypted()) {
			throw new CsvDbQException("El cifrado está desactivado. Es necesario "
					+ "establecer una contraseña para cifrar o descifrar.");
		}
		if (mode != ENCRYPT_MODE && mode != DECRYPT_MODE) {
			throw new CsvDbQException("Modo de cifrado erróneo, use uno de:" +
					System.lineSeparator() + "EncryptedTable.ENCRYPT_MODE o "
							+ "EncryptedTable.DECRYPT_MODE");
		}				
		
		var pathCsv = config.composePath(tableName);
		// La salida se manda a un fichero temporal
		Path pathCsvTemp = Util.createTempFile();		
		
		// Si se quiere encriptar se activa el modo de lectura en texto plano
		if (mode == ENCRYPT_MODE) {
			config.getCipher().setReadPlainText(true);
		}
		
		try (var csvHeaders = new CsvFileHeaders(tableName, config)) {						
			var iterator = csvHeaders.getIterator();										
			
			try (BufferedWriter bw = Files.newBufferedWriter(pathCsvTemp, StandardCharsets.UTF_8)) {						
				// Escribimos la marca de BOM de UTF-8, que es necesaria para que las aplicaciones
				// Office lo reconozcan
				bw.write("\ufeff");
				
				// Escribe la fila de cabeceras			
				bw.write(mode == ENCRYPT_MODE ? csvHeaders.toEncrypted() : csvHeaders.toString());
						
				while (iterator.hasNext()) {
					var row = iterator.next();		
					
					// Agrega la fila al resultado
					bw.write(TokensCsv.NL);
					bw.write(mode == ENCRYPT_MODE ? row.toEncrypted() : row.toString());
					
				}			
			} catch (IOException e) {
				throw new CsvDbQException(e);
			}
		} finally {
			// Comprueba si se debe desactivar el modo de lectura en texto plano
			if (mode == ENCRYPT_MODE) {
				config.getCipher().setReadPlainText(false);
			}
		}
		
		// Aplica la actualización a nivel de fichero
		try {
			Files.move(pathCsvTemp, Path.of(pathCsv), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {			
			throw new CsvDbQException(e);
		}				
	}	
}
