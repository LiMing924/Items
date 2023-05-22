package liming.texthandle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HandleReceive底层，由HandleReceive自动调用，也可直接调用
 */
public class Text {

	public static boolean DEBUG = false;// 设置显示debug信息

	private GetDataAndPacket gdap;// 消息通道

	public FileRW ENCODED = FileRW.GBK;// 消息编码和解码格式

	private DtatPoolTemps temps;// 数据合并缓冲池对象

	private int sendnum = 0, receivenum = 0, handlenum = 0, errnum = 0;// 发送数，接收数，处理数，错误数

	private int DATASIZE = setDataSize(512);// 假设总长在512传输字节的情况下，20位基本标识+5位数据段标识+127
											// 预防中文编码带来的异常和数据前缀及后缀所需长度，DATASIZE最大值为512-154=358长度

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
		temps = new DtatPoolTemps(interface1, size * coefficient);
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

	/**
	 * 
	 * @param size 每个UDP包携带的最大byte长度
	 * @return
	 */
	public int setDataSize(int size) {
		if (size <= 160)
			size += 160;
		return DATASIZE = size;
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
	 * @throws Exception
	 */

	public boolean Send(ReceiveMap receiveMap, String IP, int port, DatagramSocket socket)
			throws Exception {
		try {
			return Send(receiveMap, InetAddress.getByName(IP), port, socket);
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
	 * @throws Exception
	 */
	public boolean Send(ReceiveMap receiveMap, InetAddress address, int port, DatagramSocket socket)
			throws Exception {
		sendnum++;
		DatagramPacket[] packets = getPackets(receiveMap, address, port);
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
	 * @throws Exception
	 */
	public boolean Send(ReceiveMap receiveMap, DatagramPacket packet, DatagramSocket socket)
			throws Exception {
		return Send(receiveMap, packet.getAddress(), packet.getPort(), socket);
	}

	public JSONObject Receive(DatagramPacket packet) throws JSONException, UnsupportedEncodingException {
		receivenum++;
		String json;
		try {
			json = new String(packet.getData(), 0, packet.getLength(), ENCODED.getValue());
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

	public ReceiveMap Handle(DatagramPacket packet)
			throws Exception {
		handlenum++;
		try {
			return temps.get(packet);
		} catch (JSONException | UnsupportedEncodingException | InterruptedException | ExecutionException e) {
			errnum++;
			throw e;
		}
	}

	/**
	 * 日志输出
	 */
	void log(Object object) {
		if (DEBUG)
			gdap.writeLog(object);
	}

	private DatagramPacket[] getPackets(ReceiveMap receiveMap, InetAddress address, int port)
			throws Exception {
		List<DatagramPacket> packets = DataProcessing.getPackets(ENCODED, DATASIZE, receiveMap);
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
}