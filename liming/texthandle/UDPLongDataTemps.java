package liming.texthandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UDPLongDataTemps {
	private Map<String, UDPLongData> LongDatas = new HashMap<>();
	private UDP_Interface interface1;

	public UDPLongDataTemps(UDP_Interface interface1) {
		this.interface1 = interface1;
	}

	public List<Data> add(UDPLongDataTemps temps) {
		text("合并数据中");
		List<Data> datas = new ArrayList<>();
		for (String key : temps.LongDatas.keySet()) {
			UDPLongData data;
			if (!LongDatas.containsKey(key)) {
				data = temps.LongDatas.get(key);
			} else {
				data = LongDatas.remove(key).add(temps.LongDatas.get(key));
			}
			if (data.value()) {

				Data d = data.getData();
				text("合并完整长数据", d.getKey(), d.getValue().length());
				datas.add(d);
				data.clear();// 释放内存
			} else
				LongDatas.put(key, data);
		}
		text("合并数据完成", LongDatas.keySet());

		return datas;
	}

	public Data put(final String id, int length, int num, String key, String data) {
		if (key.endsWith("/liming/")) {// 关键字去标识化
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
		Data object = longData.put(num, data);
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

	public static class Data {
		private String key;
		private String value;

		public Data(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}
