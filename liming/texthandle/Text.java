package liming.texthandle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HandleReceive底层，由HandleReceive自动调用，也可直接调用
 */
public class Text {

	public static boolean DEBUG = false;// 设置显示debug信息

	private GetDataAndPacket gdap;// 消息通道

	public String ENCODED = "GBK";// 消息编码和解码格式

	public int SLEEP = 7;// 发送时分包参数 合适的参数可以提高性能和包占比,包占比与参数和性能成反比 过高的系数会导致计算次数增加,但包占比会更趋近于1 低系数会导致包占比低

	private UDPDataTemps temps;// 数据合并缓冲池对象

	private int sendnum = 0, receivenum = 0, handlenum = 0, errnum = 0;// 发送数，接收数，处理数，错误数

	/**
	 * 
	 * @param gdap 消息处理接口
	 * @param size 在HandleReceive中表示socket的个数
	 */
	public Text(GetDataAndPacket gdap, int size) {
		this(gdap, size, getCoefficient(size));
	}

	/**
	 * 
	 * @param gdap        消息处理接口
	 * @param size        在HandleReceive中表示socket的个数
	 * @param coefficient size系数
	 */
	public Text(GetDataAndPacket gdap, int size, int coefficient) {
		this.gdap = gdap;
		temps = new UDPDataTemps(interface1, size * coefficient);
	}

	/**
	 * 设置分包参数
	 */
	public void setSLEEP(int SLEEP) {
		this.SLEEP = SLEEP;
	}

	/**
	 * 设置消息通道
	 */
	public void setGDAP(GetDataAndPacket gdap) {
		this.gdap = gdap;
	}

	/**
	 * 定义消息处理接口
	 */
	private UDP_Interface interface1 = new UDP_Interface() {

		@Override
		public void udp_log(Object object) {
			log(object);
		}

		@Override
		public void udp_slog(Object object) {
			gdap.writeStrongLog(object);
		}
	};

	private int DATASIZE = setDataSize(512);// 假设总长在512传输字节的情况下，预防中文编码带来的异常和数据前缀及后缀所需长度，DATASIZE最大值为512-154=358长度

	/**
	 * 
	 * @param size 每个UDP包携带的最大byte长度
	 * @return
	 */
	public int setDataSize(int size) {
		if (size <= 180)
			size += 180;
		return DATASIZE = size - 162;
	}

	/**
	 * 将map转为JSONNObject
	 */
	public String getJSONString(Map<String, String> map) {
		JSONObject object = new JSONObject(map);
		return object.toString();
	}

	/**
	 * 将收到的JSONObject转为Map<String,String>
	 */
	public static Map<String, String> getMap(JSONObject object) {
		Map<String, String> map = new HashMap<>();
		for (String key : object.keySet()) {
			map.put(key, (String) object.get(key));
		}
		return map;
	}

	/**
	 * 
	 * @param map    待发送的数据
	 * @param IP     目标ip
	 * @param port   目标端口
	 * @param socket 发送的socket
	 * @return 是否发送成功
	 * @throws UnsupportedEncodingException
	 */

	public boolean Send(Map<String, String> map, String IP, int port, DatagramSocket socket)
			throws UnsupportedEncodingException {
		try {
			return Send(map, InetAddress.getByName(IP), port, socket);
		} catch (UnknownHostException e) {
			gdap.writeStrongLog(FileRW.getError(e));
			return false;
		}
	}

	/**
	 * 
	 * @param map     待发送的数据
	 * @param address 目标ip
	 * @param port    目标端口
	 * @param socket  发送的socket
	 * @return 是否发送成功
	 * @throws UnsupportedEncodingException
	 */
	public boolean Send(Map<String, String> map, InetAddress address, int port, DatagramSocket socket)
			throws UnsupportedEncodingException {
		DatagramPacket[] packets = getPackets(map, address, port);
		try {

			log("发送包长度:" + packets.length);
			System.out.println("发送包长度:" + packets.length);
			for (DatagramPacket packet : packets) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				socket.send(packet);
			}
		} catch (IOException e) {
			gdap.writeStrongLog(FileRW.getError(e));
			return false;
		}
		return true;
	}

	/**
	 * 通过数据包回发
	 * 
	 * @param map    待发送的数据
	 * @param packet UDP数据包
	 * @return 是否发送成功
	 * @throws UnsupportedEncodingException
	 */
	public boolean Send(Map<String, String> map, DatagramPacket packet, DatagramSocket socket)
			throws UnsupportedEncodingException {
		return Send(map, packet.getAddress(), packet.getPort(), socket);
	}

	/**
	 * 将Map<String, String>对象转换为DatagramPacket数组
	 * 
	 * @param map 存储字符串的Map对象
	 * @return DatagramPacket数组
	 */
	public List<DatagramPacket> getPackets(Map<String, String> map) throws UnsupportedEncodingException {
		sendnum++;
		List<JSONObject> ShortData = new ArrayList<>(); // 存放字符串形式字节码不超过DATASIZE的JSONObject
		List<JSONObject> LongData = new ArrayList<>(); // 存放字符串形式字节码超过DATASIZE的JSONObject
		List<JSONObject> AllDatas = new ArrayList<>(); // 将map的hashcode()结果转为字符串形式加上System.currentTimeMills()设置为SID

		// 遍历map中的键值对 将其分类处理
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String k = entry.getKey();
			if (k.endsWith("/liming/")) {
				k += "/liming/";
			} else if (isKey(k)) {
				k += "/liming/";
			}
			String v = entry.getValue();
			String data = "\"" + k + "\":\"" + v + "\""; // 组成"k":"v"的格式

			try {
				byte[] bytes = data.getBytes(ENCODED);
				if (bytes.length <= DATASIZE) {
					// 短数据
					JSONObject jsonObject = new JSONObject("{" + data + "}");
					ShortData.add(jsonObject);
				} else {
					// 长数据
					String[] datas = splitString(v);
					List<JSONObject> TempDatas = new ArrayList<>();
					for (int i = 0; i < datas.length; i++) {
						JSONObject o = new JSONObject();
						o.put("LONGKEY", k);
						o.put("LONGDATA", datas[i]);
						o.put("LONGLENGTH", datas.length);
						o.put("LONGNUM", i);
						TempDatas.add(o);
					}
					LongData.addAll(TempDatas);
				}
			} catch (Exception e) {
				gdap.writeStrongLog(FileRW.getError(e));
			}
		}
		// 将ShortData中的数据段的两两整合，使整合后的数据依旧满足短数据
		while (ShortData.size() > 1) {

			JSONObject jsonObject1 = ShortData.remove(0);
			JSONObject jsonObject2 = ShortData.remove(0);

			String data = (jsonObject1.toString() + jsonObject2.toString()).replace("}{", ",");
			// System.out.println(data);
			try {
				byte[] bytes = data.getBytes(ENCODED);
				if (bytes.length <= DATASIZE) {
					JSONObject jsonObject = new JSONObject(data);
					ShortData.add(jsonObject);
				} else {
					ShortData.add(jsonObject1);
					ShortData.add(jsonObject2);
					break;
				}
			} catch (Exception e) {
				gdap.writeStrongLog(FileRW.getError(e));
			}
			// System.out.println(ShortData);
		}

		// 将ShortData中的数据放入AllDatas
		AllDatas.addAll(ShortData);

		// 将LongData中的数据放入AllDatas
		AllDatas.addAll(LongData);

		// 为AllDatas中的每个JSONObject添加键"DATAID",值SID，添加键"DATALENGTH",值length，添加键"DATANUM",值num
		int num = 0;
		int length = AllDatas.size();
		String CID = System.currentTimeMillis() + "-" + map.hashCode() + "-" + new Random(map.hashCode()).nextInt(1000);
		for (JSONObject jsonObject : AllDatas) {
			jsonObject.put("DATAID", CID);
			jsonObject.put("DATALENGTH", length);
			jsonObject.put("DATANUM", num);
			num++;
		}
		List<DatagramPacket> packets = new ArrayList<>();
		num = 0;
		for (JSONObject object : AllDatas) {
			log("Text数据包:" + object);
			byte[] data = object.toString().getBytes(ENCODED);
			DatagramPacket packet = new DatagramPacket(data, data.length);
			packets.add(packet);
		}
		return packets;
	}

	public JSONObject Receive(DatagramPacket packet) throws JSONException, UnsupportedEncodingException {
		receivenum++;
		String json;
		try {
			json = new String(packet.getData(), 0, packet.getLength(), ENCODED);
		} catch (UnsupportedEncodingException e) {
			errnum++;
			throw new UnsupportedEncodingException();
		}
		JSONObject object;
		try {
			log("原数据=" + json);
			object = new JSONObject(json);
		} catch (JSONException e) {
			gdap.writeStrongLog("原数据=" + json);
			errnum++;
			throw new JSONException(e);
		}
		return object;

	}

	public Map<String, String> Handle(DatagramPacket packet)
			throws JSONException, UnsupportedEncodingException, InterruptedException, ExecutionException {
		handlenum++;
		try {
			return temps.get(Receive(packet));
		} catch (JSONException | UnsupportedEncodingException | InterruptedException | ExecutionException e) {
			errnum++;
			throw e;
		}
	}

	// 将字符串按指定长度分割
	private String[] splitString(String str) throws UnsupportedEncodingException {
		log("原数据= " + str + " length=" + str.length() + " DATASIZE=" + DATASIZE);
		List<String> parts = new ArrayList<>();
		int end = 0;
		int size = 0;
		for (int start = 0; start < str.length(); start = end) {
			if (start + DATASIZE > str.length())
				end = str.length();
			else
				end = start + DATASIZE;
			String temp = str.substring(start, end);
			int size_other = countSubstring(temp, "\\", "\"");
			while (temp.getBytes(ENCODED).length > DATASIZE - size_other) {
				int stringlength = temp.length();
				int bytelength = temp.getBytes(ENCODED).length;
				int wanting = bytelength - stringlength;
				// toText("\ttemp1:stringlength="+stringlength+",bytelength="+bytelength
				// +",wanting="+wanting+",SLEEP="+SLEEP
				// +",size_other="+size_other);
				if (wanting < SLEEP)
					wanting = SLEEP;
				size++;
				temp = temp.substring(0, stringlength - wanting / SLEEP);
				size_other = countSubstring(temp, "\\", "\"");
				// toText("\ttemp2:"+temp);
			}
			end = start + temp.length();
			parts.add(temp);
		}
		float f = 0;
		for (String s : parts.subList(0, parts.size() - 1)) {
			f += (s.getBytes(ENCODED).length * 1.0) / DATASIZE;
		}
		log("ENCODE=" + ENCODED + " SIZE=" + DATASIZE + " SLEEP=" + SLEEP + " string.length=" + str.length()
				+ " string.getbyte[].length=" + str.getBytes(ENCODED).length + " " + size + " " + parts.size() + " "
				+ f / (parts.size() - 1));
		return listToArray(parts, String[].class);
	}

	/**
	 * 日志输出
	 */
	void log(Object object) {
		if (DEBUG)
			gdap.writeLog(object);
	}

	private DatagramPacket[] getPackets(Map<String, String> map, InetAddress address, int port)
			throws UnsupportedEncodingException {
		List<DatagramPacket> packets = getPackets(map);
		for (DatagramPacket packet : packets) {
			packet.setAddress(address);
			packet.setPort(port);
		}
		return listToArray(packets, DatagramPacket[].class);
	}

	/**
	 * 将List转为数组
	 * 
	 * @param list  待转换的list
	 * @param clazz list中元素数组类型
	 * @return
	 */
	public static <T> T[] listToArray(List<T> list, Class<T[]> clazz) {
		T[] array = clazz.cast(Array.newInstance(clazz.getComponentType(), list.size()));
		return list.toArray(array);
	}

	/**
	 * 结束，释放内存
	 */
	public void clear() {
		temps.clear();
		System.gc();
		interface1.udp_slog("Text结束");
	}

	/**
	 * 计算给定字符串中出现 子串 的次数
	 * 
	 * @param str   需计算的字符串
	 * @param []sub 子串
	 * @return
	 */
	private static int countSubstring(String str, String... sub) {
		int count = 0;
		for (String s : sub) {
			int index = 0;
			while ((index = str.indexOf(s, index)) != -1) {
				count++;
				index += s.length();
			}
		}
		return count;
	}

	/**
	 * 获取Temp全部状态
	 */
	public JSONObject getState() {
		JSONObject root = new JSONObject();
		root.put("temps", getUDPDataTempsState());
		root.put("info", getStateInfo());
		return root;
	}

	/**
	 * 获取Temp中缓冲池状态
	 */
	public JSONObject getUDPDataTempsState() {
		return temps.getState();
	}

	/**
	 * 获取Temp基本参数状态
	 */
	public JSONObject getStateInfo() {
		JSONObject root = new JSONObject();
		root.put("DataSize", DATASIZE);
		root.put("Encoed", ENCODED);
		root.put("Debug", DEBUG);
		root.put("Sleep", SLEEP);
		root.put("Send", sendnum);
		root.put("Receive", receivenum);
		root.put("Handle", handlenum);
		root.put("Err", errnum);
		return root;
	}

	/**
	 * 在构造函数时使用，通过size计算参数 size系数(coefficient)
	 * 
	 * @param size
	 * @return coefficient size系数
	 */
	private static int getCoefficient(int size) {
		if (size < 3)
			size = 5;
		else if (size < 5)
			size = 4;
		else if (size < 10)
			size = 3;
		else
			size = 3;
		return size;
	}

	/**
	 * 判断 key 是否为数据包关键字
	 * 
	 * @param key 关键字
	 * @return 是否是关键字
	 */
	private static boolean isKey(String key) {
		for (String k : UDPData.keys) {
			if (key.equals(k))
				return true;
		}
		for (String k : UDPData.longkeys) {
			if (key.equals(k))
				return true;
		}
		return false;
	}
}