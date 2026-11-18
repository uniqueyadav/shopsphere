package com.shopsphere.payout.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PayoutEncryptionService {
	private static final int IV_LENGTH=12;
	private final SecretKey encryptionKey;
	public PayoutEncryptionService(SecretKey jwtKey) {
		try { byte[] material=MessageDigest.getInstance("SHA-256").digest(ByteBuffer.allocate(jwtKey.getEncoded().length+24).put(jwtKey.getEncoded()).put("shopsphere-payout-v1".getBytes(StandardCharsets.UTF_8)).array()); encryptionKey=new SecretKeySpec(material,"AES"); }
		catch(Exception e){throw new IllegalStateException("Payout account encryption is unavailable.",e);}
	}
	public String encrypt(String value) {
		try { byte[] iv=new byte[IV_LENGTH]; new SecureRandom().nextBytes(iv); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,encryptionKey,new GCMParameterSpec(128,iv)); byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length+encrypted.length).put(iv).put(encrypted).array()); }
		catch(Exception e){throw new IllegalStateException("Payout account details could not be protected.",e);}
	}
	public String decrypt(String value) {
		try { byte[] packed=Base64.getDecoder().decode(value); ByteBuffer buffer=ByteBuffer.wrap(packed); byte[] iv=new byte[IV_LENGTH]; buffer.get(iv); byte[] encrypted=new byte[buffer.remaining()]; buffer.get(encrypted); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,encryptionKey,new GCMParameterSpec(128,iv)); return new String(cipher.doFinal(encrypted),StandardCharsets.UTF_8); }
		catch(Exception e){throw new IllegalStateException("Payout account details could not be decrypted.",e);}
	}
}
