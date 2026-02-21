package es.facite.csvdbq.qlcsv;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;

import es.facite.csvdbq.security.ChaCha20Util;

public class CsvConfigCipher {		
	private String password;
	private SecretKey sk;
	private AlgorithmParameterSpec aps;
	private boolean readPlainText;
						
	public void setPassword(String password) {		
		if (password == null || password.isEmpty()) {
			this.password = null;
			sk = null;
			aps = null;
		} else {
			this.password = password;
			sk = ChaCha20Util.getKeyFromPassword(this.password);
			aps = ChaCha20Util.generateAlgorithmParameterSpec(ChaCha20Util.generateIv(sk));
		}
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean isEncrypted() {
		return password != null;
	}

	public String encrypt(String plainText) {
		return ChaCha20Util.encrypt(plainText, sk, aps);
	}
	
	public String decrypt(String cipherText) {
		return ChaCha20Util.decrypt(cipherText, sk, aps);
	}

	public boolean isReadPlainText() {
		return readPlainText;
	}

	public void setReadPlainText(boolean readPlainText) {
		this.readPlainText = readPlainText;
	}	
}
