package liming.texthandle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;

import liming.texthandle.UDPLongDataTemps.Data;

class UDPData {
    protected static final List<String> keys = Arrays.asList("DATAID", "DATALENGTH", "DATANUM");
    protected static final List<String> longkeys = Arrays.asList("LONGKEY", "LONGLENGTH", "LONGNUM", "LONGDATA");
    private long time;// 创建时间
    private Map<String, String> data;// 数据列表
    private String ID; // 总数据id
    private boolean[] values; // 包状态标识符

    private int length; // 当前包长度

    private UDP_Interface interface1;// 消息接口
    private UDPLongDataTemps temps;// 长数据缓冲列表
    private boolean equals = false;// 有效标识符

    public UDPData(UDP_Interface interface1, JSONObject object) {
        String key = object.optString(keys.get(0));
        int length = object.optInt(keys.get(1), 0);
        if (key == null || length == 0)
            return;
        try {
            init(interface1, key, length);
            put(object);
            equals = true;
        } catch (Exception e) {
            System.out.println("数据获取失败");
        }
    }

    public UDPData(UDPData data1, UDPData data2) {
        interface1 = data1.interface1;
        text("开始合并参数", data1, data1.data.keySet(), data2, data2.data.keySet());
        if (data1.equals(data2)) {
            text("合并的参数不匹配，d1:[", data1.ID, ":", data1.length, "]", "d2:[", data2.ID, ":", data2.length, "]");
            return;
        }
        time = 0;
        data = new HashMap<>();
        if (data1.equals || data2.equals) {
            if (data1.equals) {
                text("数据1有效");
                interface1 = data1.interface1;
                data.putAll(data1.data);
                values = data1.values;
                time = data1.time;
                temps = data1.temps;

            }
            if (data2.equals) {
                text("数据2有效");
                interface1 = data2.interface1;
                data.putAll(data2.data);
                if (values == null)
                    values = data2.values;
                else {
                    for (int i = 0; i < data2.length; i++)
                        if (data2.values[i])
                            values[i] = true;
                }
                if (time == 0) {
                    time = data2.time;
                } else
                    time = time > data2.time ? data2.time : time;

                if (temps == null)
                    temps = data2.temps;
                else {
                    List<Data> datas = temps.add(data2.temps);
                    for (Data data : datas) {
                        this.data.put(data.getKey(), data.getValue());
                    }
                }
            }
        } else {
            text("合并数据无效");
            return;
        }
        equals = true;
        text("合并完成", this, data.keySet());
    }

    private void init(UDP_Interface interface1, String id, int length) {
        this.length = length;
        time = System.currentTimeMillis();
        this.interface1 = interface1;
        this.ID = id;
        values = new boolean[length];
        data = new HashMap<>();
        temps = new UDPLongDataTemps(interface1);
        equals = true;
    }

    public Map<String, String> put() {
        if (value()) {
            return data;
        } else
            return null;
    }

    public boolean equals(Object object) {
        if (object instanceof UDPData) {
            UDPData o1 = (UDPData) object;
            return o1.length == length && ID == o1.ID;
        } else
            return false;
    }

    public Map<String, String> put(JSONObject object) throws InterruptedException, ExecutionException {
        text("UDPData添加数据=", object);
        object.remove(keys.get(0));
        object.remove(keys.get(1));
        int num = (int) object.remove(keys.get(2));
        values[num] = true;
        if (isLong(object.keySet())) {// 长数据处理
            text("长数据", object);
            // 从长数据处理对象缓存池中传入包中参数，得到处理结果;
            Data longdata = temps.put(ID, object.optInt(longkeys.get(1)),
                    object.optInt(longkeys.get(2)),
                    object.optString(longkeys.get(0)), object.optString(longkeys.get(3)));
            if (longdata == null) {
                text("数据缺失", object.optString(longkeys.get(0)));
                return null;
            } else {
                text("数据完整", object.optString(longkeys.get(0)));
                data.put(longdata.getKey(), longdata.getValue());
            }
        } else {// 短数据处理
            Set<String> keys = object.keySet();
            for (String key : keys) {
                String k;
                if (key.endsWith("/liming/")) {
                    k = key.substring(0, key.length() - "/liming/".length());
                } else
                    k = key;
                data.put(k, object.optString(key));
            }
            text("短数据添加", keys);
        }
        text(this);
        if (value()) {
            return data;
        } else
            return null;
    }

    private static boolean isLong(Set<String> set) {
        if (set.size() != 4)
            return false;
        for (String key : set) {
            if (!longkeys.contains(key))
                return false;
        }
        return true;
    }

    public boolean value() {
        for (boolean b : values) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean[] values() {
        return values;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getID() {
        return ID;
    }

    public Map<String, String> clear() {
        Map<String, String> d = new HashMap<>(data);
        data.clear();
        temps.clear();
        return d;
    }

    @Override
    public String toString() {
        return "UDPData [data=" + data + ", ID=" + ID + ", values=" + Arrays.toString(values) + "]";
    }

    private void text(Object... objects) {
        String str = "\tUDPData:{";
        for (Object object : objects) {
            str += " " + object.toString();
        }
        interface1.udp_log(str + "}");
    }

    public long getTime() {
        return time;
    }
}
