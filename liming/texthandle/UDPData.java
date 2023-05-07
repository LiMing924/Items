package liming.texthandle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;

class UDPData {
    protected static final List<String> keys = Arrays.asList("DATAID", "DATALENGTH", "DATANUM");
    protected static final List<String> longkeys = Arrays.asList("LONGKEY", "LONGLENGTH", "LONGNUM", "LONGDATA");
    private long time;
    private Map<String, String> data;
    private String ID;
    private boolean[] values;

    private UDP_Interface interface1;
    private UDPLongDataTemps temps;

    public UDPData(UDP_Interface interface1, String id, int length) {
        time = System.currentTimeMillis();
        this.interface1 = interface1;
        this.ID = id;
        values = new boolean[length];
        data = new HashMap<>();
        temps = new UDPLongDataTemps(interface1);
    }

    public Map<String, String> put(JSONObject object) throws InterruptedException, ExecutionException {
        text("UDPData添加数据=", object);
        object.remove(keys.get(0));
        object.remove(keys.get(1));
        int num = (int) object.remove(keys.get(2));
        values[num] = true;
        if (isLong(object.keySet())) {
            text("长数据", object);
            JSONObject longdata = temps.put(ID, object.optInt(longkeys.get(1)),
                    object.optInt(longkeys.get(2)),
                    object.optString(longkeys.get(0)), object.optString(longkeys.get(3)));
            if (longdata == null) {
                text("数据缺失", object.optString(longkeys.get(0)));
                return null;
            } else {
                text("数据完整", object.optString(longkeys.get(0)));
                String firstKey = longdata.names().getString(0);
                String firstValue = longdata.getString(firstKey);
                data.put(firstKey, firstValue);
            }
        } else {
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

    public Map<String, String> getData() {
        return data;
    }

    public String getID() {
        return ID;
    }

    public void clear() {
        data.clear();
        temps.clear();
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
