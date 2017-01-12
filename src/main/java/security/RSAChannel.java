package security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.util.encoders.Base64;

public class RSAChannel extends SecureChannelDecorator {

	private Cipher cipher;
	private PublicKey pubKey;
	private PrivateKey privKey;
	
	public RSAChannel(SecureChannel bC, PublicKey pubKey, PrivateKey privKey) throws EncryptionException  {
		super(bC);
		
		this.pubKey = pubKey;
		this.privKey = privKey;
		this.initRSA();
	}
	public RSAChannel(SecureChannel bC, PrivateKey privKey) throws EncryptionException  {
		super(bC);
				
		this.privKey = privKey;
		this.initRSA();
	}
	
	/**
	 * encrypts message with RSA and sends it afterwards
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	@Override
	public void sendMessage(byte[] message) throws EncryptionException
	{
		if(this.pubKey == null)
		{
			throw new EncryptionException("Public Key not set!");
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
			throw new EncryptionException("Received message has been null!");
		}
		byte[] messageBytes =   Base64.decode(base64String);			
		
		if(privKey == null)
		{
			throw new EncryptionException("Private Key not set!");
		}
		//try to decrypt using the own private key		
		try {
			//decrypt and return the message
			return new String(decrypt(messageBytes),Charset.forName("UTF-8"));
		} catch (InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			throw new EncryptionException(e.getMessage());
		}
		
	}
	public void initRSA() throws EncryptionException
	{
		//initialize cipher for RSA
		try
		{
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");	
		}
		catch(Exception e)
		{
			throw new EncryptionException(e.getMessage());
		}
	}
	
	
	public void setPublicKey(PublicKey pubKey)
	{
		this.pubKey = pubKey;
	}
	
	private byte[] decrypt(byte[] msg) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException
	{
		//initialiue cipher with private key and set mode to decryption
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] plainText = cipher.doFinal(msg);
		return plainText;
	}
	
	
	private byte[] encrypt(byte[] msg) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException
	{
		//initialiue cipher with public key and set mode to encryption
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);			
		byte[] cipherText = cipher.doFinal(msg);
		return cipherText;
	}
}
