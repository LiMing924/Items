package liming.texthandle;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

class UDPLongDataTemps {
	private Map<String, UDPLongData> LongDatas = new HashMap<>();
	private UDP_Interface interface1;

	public UDPLongDataTemps(UDP_Interface interface1) {
		this.interface1 = interface1;
	}

	public JSONObject put(final String id, int length, int num, String key, String data) {
		if (key.endsWith("/liming/")) {
			key = key.substring(0, key.length() - "/liming/".length());
		}
		text("添加长数据数据：", id, length, num, key, data);
		UDPLongData longData;
		if (LongDatas.containsKey(id + key)) {
			text("长数据已有记录：", id + key, "正在获取");
			longData = LongDatas.remove(id + key);
		} else {
			text("长数据首次接收：", id + key, "正在新建");
			longData = new UDPLongData(interface1, key, length);
		}
		JSONObject object = longData.put(num, data);
		if (object != null) {
			text("当前长数据接收完：", object);
			longData.clear();
			return object;
		}
		text("当前长数据未接收完：", longData);
		LongDatas.put(id + key, longData);
		return null;
	}

	public void clear() {
		LongDatas.clear();
	}

	public Map<String, UDPLongData> getMap() {
		return LongDatas;
	}

	private void text(Object... objects) {
		String str = "\t\tUDPLongTemps:{";
		for (Object object : objects) {
			str += " " + object.toString();
		}
		interface1.udp_log(str + "}");
	}
}
