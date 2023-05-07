package liming.texthandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import liming.key.encryption.RSA_Encryption;

public enum FileRW {
	UTF8("UTF-8"), GBK("GBK"), GB2312("GB2312"), GB18030("GB18030"), ISO88591("ISO-8859-1"), UTF16("UTF-16");

	private String value;

	FileRW(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	private static FileRW CODE = GBK;

	/**
	 * 设置编码格式
	 * 
	 * @param code
	 * @return
	 */
	public static String setCode(FileRW code) {
		CODE = code;
		return CODE.value;
	}

	/**
	 * 读取文件
	 * 
	 * @param file 目标文件
	 * @return 文件内容
	 */
	public static String Read(File file) {
		try {
			BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), CODE.value));// new一个BufferedReader对象，将文件内容读取到缓存
			StringBuilder sb = new StringBuilder();// 定义一个字符串缓存，将字符串存放缓存中
			String s = "";
			while ((s = bReader.readLine()) != null) {// 逐行读取文件内容，不读取换行符和末尾的空格
				sb.append(s + "\n");// 将读取的字符串添加换行符后累加存放在缓存中
			}
			bReader.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String();
	}

	/**
	 * 写入文件，采用覆盖的方式
	 * 
	 * @param file 目标文件
	 * @param code 写入内容
	 * @return 写入结果
	 */
	public static boolean Write(File file, String code) {
		try {

			OutputStreamWriter oStreamWriter = new OutputStreamWriter(new FileOutputStream(file), CODE.value);
			oStreamWriter.append(code);
			oStreamWriter.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 将错误信息的堆栈数据转为文本显示
	 * 
	 * @param e
	 * @return
	 */
	public static String getError(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	// 读取文件并将二进制数据转为字符串
	public static String readFile(String filePath) {
		String result = "";
		try {
			File file = new File(filePath);
			if (file.length() > Integer.MAX_VALUE) {
				System.out.println("文件大小超过限制" + file.length() + ":" + Integer.MAX_VALUE
						+ "，若本地复制文件请使用transferFiles(oldfilepath,newfilepath)");
				return null;
			}
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			result = RSA_Encryption.signatureToString(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// 将文件从一个地方转移到另一个地方
	public static boolean transferFiles(String filePath, String newFilePath) {
		try {
			File file = new File(filePath);
			FileInputStream inputStream = new FileInputStream(file);
			FileOutputStream outputStream = new FileOutputStream(newFilePath);
			byte[] buffer = new byte[102400];
			int length;
			while ((length = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}
			inputStream.close();
			outputStream.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 将字符串转为二进制数据并写入文件
	public static boolean writeFile(String filePath, String content, boolean append) {
		try {
			File file = new File(filePath);
			FileOutputStream fos = new FileOutputStream(file, append);
			byte[] data = RSA_Encryption.stringToSignature(content);
			fos.write(data);
			fos.flush();
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * @param sourceFilePath 目标文件
	 * @param destFilePath   新文件
	 * @throws IOException
	 */
	public static void copyLargeFile(String sourceFilePath, String destFilePath) throws IOException {
		Path sourcePath = Paths.get(sourceFilePath);
		Path destPath = Paths.get(destFilePath);
		Files.copy(sourcePath, destPath);
	}
}
