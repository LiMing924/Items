package liming.texthandle;

import java.util.Arrays;

import org.json.JSONObject;

class UDPLongData {
    private String longkey;
    private boolean[] values;
    private String[] datas;
    private UDP_Interface interface1;

    public UDPLongData(UDP_Interface interface1, String key, int length) {
        this.interface1 = interface1;
        longkey = key;
        values = new boolean[length];
        datas = new String[length];
    }

    public JSONObject put(int num, String data) {
        values[num] = true;
        datas[num] = data;
        text(this);
        if (value()) {
            JSONObject value = new JSONObject();
            value.put(longkey, getData());
            return value;
        }
        return null;
    }

    private boolean value() {
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

    private String getData() {
        StringBuilder sb = new StringBuilder();
        for (String data : datas) {
            sb.append(data);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "UDPLongData [longkey=" + longkey + ", values=" + Arrays.toString(values) + ", datas="
                + Arrays.toString(datas) + "]";
    }

    public void clear() {
        longkey = null;
        values = null;
        datas = null;
    }

    private void text(Object... objects) {
        String str = "\t\t\tUDPLongData:{";
        for (Object object : objects) {
            str += " " + object.toString();
        }
        interface1.udp_log(str + "}");
    }
}
