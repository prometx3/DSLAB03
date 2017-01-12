package security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

public class AESChannel extends SecureChannelDecorator{

	private Cipher cipher;	
	private byte[] IVParameter;	
	private SecretKey secretKey;
	
	public AESChannel(SecureChannel bC, String secretKeyString, String IVParameterString) throws EncryptionException  {
		super(bC);
		
		//convert the secret and IV strings 
		byte[] secretKeyByte = Base64Channel.decodeBase64Byte(secretKeyString);
		this.secretKey = new SecretKeySpec(secretKeyByte, 0, secretKeyByte.length, "AES");		
		this.IVParameter = Base64Channel.decodeBase64Byte(IVParameterString);
		this.initASE();
	}
	
	
	/**
	 * encrypts message with RSA and sends it afterwards
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	@Override
	public void sendMessage(byte[] message) throws EncryptionException
	{
		if(this.secretKey == null || this.IVParameter == null)
		{
			throw new EncryptionException("Secret Key or IV-Parameter not set!");
		}
		try{
			this.basicChannel.sendMessage(encrypt(message));
		}
		catch(Exception e)
		{
			throw new EncryptionException(e.getMessage());
		}
	}

	
	/**
	 * receives the base64 encoded message, decrypts it and returns the string
	 * @throws IOException 
	 */
	public String receiveMessage() throws EncryptionException, IOException
	{
		//decrypt message from base64
		String base64String = this.basicChannel.receiveMessage();	
		if(base64String == null)
		{
			throw new EncryptionException("Message has been null!");
		}
		byte[] messageBytes =   Base64.decode(base64String);			
		
		if(this.secretKey == null || this.IVParameter == null)
		{
			throw new EncryptionException("Private Key not set!");
		}
		//try to decrypt using the own private key		
		try {
			//decrypt and return the message
			return new String(decrypt(messageBytes),Charset.forName("UTF-8"));
		} catch (InvalidKeyException | IllegalBlockSizeException |InvalidAlgorithmParameterException 
				| BadPaddingException e) {
			throw new EncryptionException(e.getMessage());
		}
		
	}
	public void initASE() throws EncryptionException
	{
		//initialize cipher for RSA
		try
		{
			cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");	
		}
		catch(Exception e)
		{
			throw new EncryptionException(e.getMessage());
		}
	}
	
	
	public void setSecretAndIV(SecretKey secretKey, byte[] IV)
	{
		this.secretKey = secretKey;
		this.IVParameter = IV;
	}
	
	private byte[] decrypt(byte[] msg) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException
	{
		//initialiue cipher with secret key and IV parameter and set mode to decryption
		AlgorithmParameterSpec IVspec = new IvParameterSpec(IVParameter);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, IVspec);
		byte[] plainText = cipher.doFinal(msg);
		return plainText;
	}
	
	
	private byte[] encrypt(byte[] msg) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
	{
		//initialiue cipher with secret key and IV parameter and set mode to encryption
		AlgorithmParameterSpec IVspec = new IvParameterSpec(IVParameter);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, IVspec);
		byte[] cipherText = cipher.doFinal(msg);
		return cipherText;
	}

}
