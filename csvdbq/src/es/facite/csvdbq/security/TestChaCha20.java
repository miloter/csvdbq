package es.facite.csvdbq.security;

import java.io.File;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;

public class TestChaCha20 {
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		System.out.println("Clave secreta AES-256:");
		final SecretKey sk = ChaCha20Util.getKeyFromPassword("Primo-23");
		System.out.println(new String(sk.getEncoded()));

		System.out.println("Vector de inicialización:");
		final byte[] iv = ChaCha20Util.generateIv(sk);
		System.out.println(new String(iv));

		System.out.println("AlgorithmParameterSpec: ");
		final AlgorithmParameterSpec aps = ChaCha20Util.generateAlgorithmParameterSpec(iv);
		System.out.println(aps);

		final String strToCipher = "\"¡Hola Mundo del \"\"€uro\"\"!\"";
		// final String strToCipher = "null";
		System.out.println("Cadena en texto plano: " + strToCipher);
		System.out.println("Cadena en texto cifrado: " + ChaCha20Util.encrypt(strToCipher, sk, aps));

		final long tStart = System.currentTimeMillis();

		for (int i = 0; i < 10_000; i++) {
			String cipherText = ChaCha20Util.encrypt(strToCipher, sk, aps);
			// System.out.println("Texto cifrado: " + cipherText);

			String uncipherText = ChaCha20Util.decrypt(cipherText, sk, aps);
			// System.out.println("Texto descifrado: " + uncipherText);
		}
		
		var fileUncipher = new File("texto-plano.txt");
		var fileCipher = new File("texto-cifrado.txt");
		ChaCha20Util.encryptFile(fileUncipher, fileCipher, sk, aps);
		fileUncipher.delete();
		ChaCha20Util.decryptFile(fileCipher, fileUncipher, sk, aps);
		

		System.out.println("Procesado en " + (System.currentTimeMillis() - tStart) + " ms.");
	}
}
