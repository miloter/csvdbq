package es.facite.csvdbq.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Clase de utilidad para cifrar/descifrar usando el algoritmo ChaCha20.
 */
public class ChaCha20Util {
	private static final String KEY_SPEC_ALGORITHM = "AES";
	private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int KEY_ITER = 65536;
	private static final int KEY_BITS = 256;
	private static final String CIPHER_ALGORITHM = "ChaCha20";
	private static final int IV_BYTES_LENGTH = 12;
	private static final int FILE_BUFFER_SIZE = 64;

	/**
	 * Genera una clave secreta AES de 256 bits a partir de una contraseña.
	 * 
	 * @param password
	 * @return
	 */
	public static SecretKey getKeyFromPassword(String password) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
			// El salt se deriva de la propia password
			KeySpec spec = new PBEKeySpec(password.toCharArray(), password.getBytes(), KEY_ITER, KEY_BITS);
			SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_SPEC_ALGORITHM);
			return secret;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Genera un IV para generar distintos textos cifrados con la misma clave
	 * aplicada al mismo texto.
	 * 
	 * @param sk Los 32 bytes de la clave secreta que se usarán para generar el
	 *           vector de inicialización.
	 * @return
	 */
	public static byte[] generateIv(SecretKey sk) {
		byte[] encoded = sk.getEncoded();
		byte[] iv = new byte[IV_BYTES_LENGTH];
		
		for (int i = 0; i < IV_BYTES_LENGTH; i++) {
			iv[i] = encoded[i];
		}
		
		return iv;
	}
	
	/**
	 * Genera una especificación de parámetros de cifrado para generar distintos textos cifrados
	 * en cada bloque de cifrado, con la misma clave secreta aunque los bloques de texto plano sean
	 * iguales.
	 * 
	 * @param iv Vector de inicialización.
	 * @return
	 */
	public static AlgorithmParameterSpec generateAlgorithmParameterSpec(byte[] iv) {		
		return new ChaCha20ParameterSpec(iv, 1);
	}

	/**
	 * Devuelve una cadena de salida cifrada codificada en base 64, a partir de una
	 * cadena de entrada en texto plano, una clave secreta y una especificación de
	 * parámetros de cifrado.
	 * 
	 * @param input
	 * @param key
	 * @param aps
	 * @return
	 */
	public static String encrypt(String input, SecretKey key, AlgorithmParameterSpec aps) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, aps);
			byte[] cipherText = cipher.doFinal(input.getBytes());

			return Base64.getEncoder().encodeToString(cipherText);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Devuelve una cadena de salida en texto plano, a partir de una cadena cifrada
	 * en base 64, una clave secreta y una especificación de parámetros de cifrado.
	 * 
	 * @param cipherText
	 * @param key
	 * @param iv
	 * @return
	 */
	public static String decrypt(String cipherText, SecretKey key, AlgorithmParameterSpec aps) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, aps);
			byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));

			return new String(plainText);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Devuelve una fichero de salida cifrado, a partir de un fichero de entrada en texto
	 * plano, una clave secreta y una especificación de parámetros de cifrado.
	 * @param inputFile
	 * @param outputFile
	 * @param key
	 * @param aps
	 */
	public static void encryptFile(File inputFile, File outputFile, SecretKey key, AlgorithmParameterSpec aps) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, aps);
			FileInputStream inputStream = new FileInputStream(inputFile);
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			byte[] buffer = new byte[FILE_BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				byte[] output = cipher.update(buffer, 0, bytesRead);
				if (output != null) {
					outputStream.write(output);
				}
			}
			byte[] outputBytes = cipher.doFinal();
			if (outputBytes != null) {
				outputStream.write(outputBytes);
			}
			inputStream.close();
			outputStream.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Devuelve una fichero de salida en texto plano, a partir de un fichero de entrada
	 * cifrado, una clave secreta y una especificación de parámetros de cifrado.
	 * @param inputFile
	 * @param outputFile
	 * @param key
	 * @param aps
	 */
	public static void decryptFile(File inputFile, File outputFile, SecretKey key, AlgorithmParameterSpec aps) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, aps);
			FileInputStream inputStream = new FileInputStream(inputFile);
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			byte[] buffer = new byte[FILE_BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				byte[] output = cipher.update(buffer, 0, bytesRead);
				if (output != null) {
					outputStream.write(output);
				}
			}
			byte[] outputBytes = cipher.doFinal();
			if (outputBytes != null) {
				outputStream.write(outputBytes);
			}
			inputStream.close();
			outputStream.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
