package liming.texthandle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class UDPByteDataInfo {
    private byte[] data;// 包内有效数据
    private int length;// 包总长
    private int num;// 包序号
    private String key;// 包key
    private UDPByteDataInfo[] byteInfos;// 缓冲队列
    private int comst = 1;// 计数器

    UDPByteDataInfo(byte[] data, int length, int num, String key) {
        this.data = data;
        this.length = length;
        this.num = num;
        this.key = key;
    }

    public void init() {
        if (byteInfos == null) {
            byteInfos = new UDPByteDataInfo[length];
            byteInfos[num] = this;
        }
    }

    public void add(UDPByteDataInfo info) {
        comst++;
        byteInfos[info.num] = info;
    }

    public boolean isValid() {
        if (comst < length || byteInfos == null) {
            return false;
        }
        for (int i = length - 1; i >= 0; i--) {
            if (byteInfos[i] == null)
                return false;
        }
        return true;
    }

    public void getData(ReceiveMap map) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = 0; i < length; i++) {
            outputStream.write(byteInfos[i].data);
        }
        map.put(key, outputStream.toByteArray());
        outputStream.close();
    }

    @Override
    public String toString() {
        return "ByteInfo [data=" + data.length + ", length=" + length + ", num=" + num + ", key="
                + key + "]";
    }
}
