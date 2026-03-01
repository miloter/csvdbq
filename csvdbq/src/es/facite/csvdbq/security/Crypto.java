package es.facite.csvdbq.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Crypto {
	/**
	 * Devuelve el hash MD5 de la cadena de entrada.
	 * 
	 * @param input
	 * @return
	 */
	public static String md5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buffer = md.digest(input.getBytes());
			BigInteger bi = new BigInteger(1, buffer);
			String hash = bi.toString(16);

			while (hash.length() < 32) {
				hash = "0" + hash;
			}

			return hash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Devuelve el hash SHA-256 (SHA-2) de la cadena de entrada.
	 * 
	 * @param input
	 * @param suppressLeadingZeros Parámetro que indica si se suprimen o no
	 * los ceros iniciales. Por ejemplo, si es true, los bytes {0xff, 0xa} se
	 * devuelven como "ffa", pero si es false, como "ff0a". 
	 * @return
	 */
	public static String sha256(String input, boolean suppressLeadingZeros) {
		try {
			byte[] bytes = input.getBytes();
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes);
			bytes = md.digest();
			return toHex(bytes, suppressLeadingZeros);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Convierte un array de bytes a una cadena hexadecimal.
	 * 
	 * @param bytes
	 * @param suppressLeadingZeros Parámetro que indica si se suprimen o no
	 * los ceros iniciales. Por ejemplo, si es true, los bytes {0xff, 0xa} se
	 * devuelven como "ffa", pero si es false, como "ff0a". 
	 * @return
	 */
	public static String toHex(byte[] bytes, boolean suppressLeadingZeros) {
		final String FORMAT = suppressLeadingZeros ? "%x" : "%02x";
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {			
			sb.append(String.format(FORMAT, b));
		}

		return sb.toString();
	}

	/**
	 * Aplica un cifrado o descifrado a un texto por el método César: método usado
	 * por el Emperador Romano Julio César para comunicarse con sus Generales.
	 * 
	 * @param {string}  text Texto que será cifrado o descifrado.
	 * @param {number}  key Clave numérica de cifrado o descifrado (1 a 92).
	 * @param {boolean} encode 'true' para cifrar y 'false' para descifrar.
	 */
	public static String code(String text, int key, boolean encode) {
		final String ALPHA = "!\"#$%&'()*+,-./0123456789:;<=>?ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
		final int ALPHA_SIZE = ALPHA.length();
		StringBuilder s = new StringBuilder();
		char c;
		int pos, i;

		for (i = 0; i < text.length(); i++) {
			c = text.charAt(i);
			pos = ALPHA.indexOf(c);
			if (pos >= 0) {
				if (encode) {
					pos = (pos + key) % ALPHA_SIZE;
				} else {
					pos = (pos - key) % ALPHA_SIZE;
					if (pos < 0) {
						pos = ALPHA_SIZE + pos;
					}
				}
				s.append(ALPHA.charAt(pos));
			} else {
				s.append(c); // Se agrega sin cifrar ni descifrar al no estar en el alfabeto
			}
		}

		return s.toString();
	}

	/**
	 * Devuelve un password de una longitud dada incluyendo de forma aleatoria
	 * letras minúsculas, mayúsculas, dígitos y los caracteres: (-: \u002d), (.:
	 * \u002e), (_: \u005f). Si la longitud es cero o negativa se devuelve por
	 * defecto una contraseña de 8 caracteres.
	 *
	 * @param length
	 * @return String
	 */
	public static String getPassword(int length) {
		final int DEFAULT = 8;
		final String chars = "\u002d\u002e\u005f";

		if (length <= 0) {
			length = DEFAULT;
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			int clase = randInt(1, 4);
			switch (clase) {
			case 1: // Minúculas
				sb.append((char) randInt(97, 122));
				break;
			case 2: // Mayúsculas
				sb.append((char) randInt(65, 90));
				break;
			case 3: // Dígitos
				sb.append((char) randInt(48, 57));
				break;
			case 4: // Caracteres especiales
				sb.append(chars.charAt(randInt(0, chars.length() - 1)));
				break;
			}
		}

		return sb.toString();
	}

	/**
	 * Devuelve una cadena alfanumérica en base 36 formada por dígitos y letras
	 * mayúsculas.
	 *
	 * @param length
	 * @return
	 */
	public static String alphaNum36(int length) {
		final String ALPHA = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			sb.append(ALPHA.charAt(randInt(0, ALPHA.length() - 1)));
		}

		return sb.toString();
	}
			
	/**
	 * Devuelve un número entero aleatorio entre dos valores.
	 *
	 * @param min valor mínimo incluido.
	 * @param max valor máximo incluido.
	 * @return int
	 */
	public static int randInt(int min, int max) {
		return (int) ((max - min + 1) * Math.random() + min);
	}

	/**
	 * Devuelve un número decimal aleatorio entre dos valores.
	 *
	 * @param min valor mínimo incluido.
	 * @param max valor máximo incluido.
	 * @return double.
	 */
	public static double randDec(double min, double max) {
		return (max - min) * Math.random() + min;
	}

	/**
	 * Ordena aleatoriamente una matriz con una buena distribución aleatoria por el
	 * método Fisher Yates, descubierto en 1938.
	 * 
	 * @param arr Array de elementos que se mezclarán.
	 * @return El array mezclado.
	 */
	public static int[] shuffle(int[] arr) {
		for (int i = arr.length - 1; i > 0; i--) {
			int j = (int) Math.floor(Math.random() * i);
			int k = arr[i];
			arr[i] = arr[j];
			arr[j] = k;
		}

		return arr;
	}
}
