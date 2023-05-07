package liming.key.encryption;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;

public class RSA_Encryption {
	public KeyPairGenerator keyPairGenerator;
	public KeyPair keyPair;
	public PublicKey publicKey;
	public PrivateKey privateKey;
	public int key_size = 2048;

	public RSA_Encryption() {

	}

	public RSA_Encryption(int size) {
		key_size = size;
	}

	public KeyPairGenerator getKeyPairGenerator() {
		if (keyPairGenerator == null)
			try {
				keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
				keyPairGenerator.initialize(key_size); // 指定密钥长度
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			}
		return keyPairGenerator;
	}

	public KeyPair getKeyPair() {
		if (keyPair == null)
			keyPair = getKeyPairGenerator().generateKeyPair();
		return keyPair;
	}

	public PublicKey getPublicKey() {
		if (publicKey == null)
			publicKey = getKeyPair().getPublic();
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		if (privateKey == null)
			privateKey = getKeyPair().getPrivate();
		return privateKey;
	}

	public static String ALGORITHM = "RSA";
	private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final Random RANDOM = new Random();

	/**
	 * 使用私钥对输入字符串进行签名
	 * 
	 * @param str        输入字符串
	 * @param privateKey 私钥
	 * @return 签名结果
	 */
	public static byte[] encrypt(String str, PrivateKey privateKey) {
		byte[] result = null;
		try {
			Signature signature = Signature.getInstance("SHA256withRSA"); // 使用SHA256withRSA签名算法
			signature.initSign(privateKey); // 使用私钥进行签名
			signature.update(str.getBytes()); // 对输入字符串进行签名
			result = signature.sign(); // 生成签名结果
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 使用公钥对明文进行加密
	 * 
	 * @param str       明文
	 * @param publicKey 公钥
	 * @return 加密后的密文
	 * @throws Exception
	 */
	public static byte[] encode(String str, PublicKey publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 使用私钥对密文进行解密
	 * 
	 * @param bytes      密文
	 * @param privateKey 私钥
	 * @return 解密后的明文
	 * @throws Exception
	 */
	public static String decode(byte[] bytes, PrivateKey privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decryptedBytes = cipher.doFinal(bytes);
		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}

	/**
	 * 将签名转换为字符串
	 * 
	 * @param signature 签名字节数组
	 * @return 签名字符串
	 */

	public static String signatureToString(byte[] signature) {
		return Base64.getEncoder().encodeToString(signature);
	}

	/**
	 * 将字符串表示的签名转换为字节数组
	 * 
	 * @param signatureString 签名字符串
	 * @return 签名字节数组
	 */
	public static byte[] stringToSignature(String signatureString) {
		return Base64.getDecoder().decode(signatureString);
	}

	/**
	 * 使用公钥对输入字符串进行验证
	 * 
	 * @param str       输入字符串
	 * @param signature 签名结果
	 * @param publicKey 公钥
	 * @return 验证结果
	 */
	public static boolean decrypt(String str, byte[] signature, PublicKey publicKey) {
		boolean result = false;
		try {
			Signature verifier = Signature.getInstance("SHA256withRSA"); // 使用SHA256withRSA签名算法
			verifier.initVerify(publicKey); // 使用公钥进行验证
			verifier.update(str.getBytes()); // 对输入字符串进行验证
			result = verifier.verify(signature); // 验证签名结果
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 将公钥转换为字符串格式
	 * 
	 * @param publicKey 公钥
	 * @return 公钥字符串
	 */
	public static String KeyToString(PublicKey publicKey) {
		return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	}

	/**
	 * 将私钥转换为字符串格式
	 * 
	 * @param privateKey 私钥
	 * @return 私钥字符串
	 */
	public static String KeyToString(PrivateKey privateKey) {
		return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	}

	/**
	 * 将字符串格式的公钥转换为公钥对象
	 * 
	 * @param publicKeyString 公钥字符串
	 * @return 公钥对象
	 */
	public static PublicKey stringToPublicKey(String publicKeyString) {
		PublicKey publicKey = null;
		try {
			byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
			publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKeyBytes));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return publicKey;
	}

	/**
	 * 将字符串格式的私钥转换为私钥对象
	 * 
	 * @param privateKeyString 私钥字符串
	 * @return 私钥对象
	 */
	public static PrivateKey stringToPrivateKey(String privateKeyString) {
		PrivateKey privateKey = null;
		try {
			byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
			privateKey = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return privateKey;
	}

	public static String generate(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
		}
		return sb.toString();
	}
}
