package liming.texthandle;

import java.util.Arrays;

import liming.texthandle.UDPLongDataTemps.Data;

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

    public UDPLongData add(UDPLongData data) {
        text("单个长数据合并中", "合并前", Arrays.toString(values), " : ", Arrays.toString(data.values));
        for (int i = 0; i < values.length; i++) {
            if (data.values[i]) {
                datas[i] = data.datas[i];
                values[i] = data.values[i];
            }
        }
        text("单个长数据合并完成", "合并后", Arrays.toString(values));

        return this;
    }

    public Data put(int num, String data) {
        values[num] = true;
        datas[num] = data;
        text(this);
        if (value()) {
            return getData();
        }
        return null;
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

    public Data getData() {
        return new Data(longkey, getdata());
    }

    private String getdata() {
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
