package liming.texthandle;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import liming.key.encryption.RSA_Encryption;

class UDPDataInfo {
    private FileRW ENCODE;// 编码
    private long time;// 数据发送时间
    private long startTime, lastTime, endTime;// 首次收到的时间,最后一次收到的时间，处理结束的时间
    private int datasize;// 缓冲区长度
    private int code;// 随机码
    private int hashCode;
    private int length;// 数据总长
    private int num;// 当前长度
    private int MajorVersionNumber;// 主版本号
    private int MinorVersionNumber;// 副版本号
    private int SerialNumber;// 序列号

    private byte[] data;// 这个包携带的数据

    public UDPDataInfo(DatagramPacket packet) throws Exception {
        this(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
    }

    private UDPDataInfo(FileRW ENCODE, long time, long startTime, long lastTime, long endTime, int datasize, int code,
            int hashCode, int length, int majorVersionNumber, int minorVersionNumber, int serialNumber) {
        this.ENCODE = ENCODE;
        this.time = time;
        this.startTime = startTime;
        this.lastTime = lastTime;
        this.endTime = endTime;
        this.datasize = datasize;
        this.code = code;
        this.hashCode = hashCode;
        this.length = length;
        MajorVersionNumber = majorVersionNumber;
        MinorVersionNumber = minorVersionNumber;
        SerialNumber = serialNumber;
    }

    public UDPDataInfo(byte[] info) throws Exception {
        if (info.length < 20) {
            throw new Exception("非本协议的数据包,终止处理,其中Bast64的结果为: " + RSA_Encryption.signatureToString(info));
        }
        startTime = lastTime = endTime = System.currentTimeMillis();
        ENCODE = FileRW.getFileRW(info[0] >>> 4 & 0x0f);
        time = (info[0] & 0x0fL) << 38 | (info[1] & 0xffL) << 30 | (info[2] & 0xffL) << 22 | (info[3] & 0xffL) << 14
                | (info[4] & 0xffL) << 6 | (info[5] & 0xffL) >>> 2;
        code = (info[5] & 0x03) << 8 | (info[6] & 0xff);
        hashCode = (info[7] & 0xff) << 24 | (info[8] & 0xff) << 16 | (info[9] & 0xff) << 8 | (info[10] & 0xff);
        length = (info[11] & 0xff) << 8 | (info[12] & 0xff);
        num = (info[13] & 0xff) << 8 | (info[14] & 0xff);
        datasize = (info[15] & 0xff) << 8 | (info[16] & 0xff);
        MajorVersionNumber = (info[17] & 0xff);
        MinorVersionNumber = (info[18] & 0xff);
        SerialNumber = (info[19] & 0xff);
        data = Arrays.copyOfRange(info, 20, info.length);
    }

    // 得到包标识
    public String getID() {
        return time + "-" + code + "-" + hashCode;
    }

    private int comst = 1;
    private UDPDataInfo[] infos;
    private byte[] value;

    protected boolean isValid() {
        // System.out.println(comst + " " + length);
        if (length == 1) {
            return true;
        }
        if (comst < length || infos == null)
            return false;
        for (int i = value.length - 1; i > 0; i--) {
            if (i == value.length - 1) {
                for (int j = 0; j < length % 8; j++) {
                    if ((value[i] >>> j & 0x01) == 0) {
                        // System.out.println(i * 8 + j + "缺失");
                        return false;
                    }
                }
            } else {
                if (value[i] != (byte) 255) {
                    for (int j = 0; j < 8; j++) {
                        if ((value[i] >>> j & 0x01) == 0) {
                            return false;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    protected ReceiveMap getReceiveMap() throws Exception {
        ReceiveMap map = new ReceiveMap();

        if (length == 1) {
            int type = data[0] >>> 7 & 0x01;
            if (type == 0) {
                // String string = new String(Arrays.copyOfRange(data, 1, data.length),
                // ENCODE.getValue());
                // System.out.println(string);
                JSONObject object = new JSONObject(
                        new String(Arrays.copyOfRange(data, 1, data.length), ENCODE.getValue()));
                for (String key : object.keySet()) {
                    if (key.equals("JSONS")) {
                        JSONObject jsonObject = new JSONObject(object.getString(key));
                        for (String jsonkey : jsonObject.keySet()) {
                            map.put(jsonkey, jsonObject.getJSONObject(jsonkey));
                        }
                    } else {
                        map.put(key, object.optString(key));
                    }
                }
            } else {
                int keylength = data[0] & 0x7f;
                String key = new String(data, 5, keylength, ENCODE.getValue());
                map.put(key, Arrays.copyOfRange(data, 5 + keylength, data.length));
            }
            {
                UDPDataInfo info = new UDPDataInfo(ENCODE, time, startTime, lastTime, System.currentTimeMillis(),
                        datasize, code, hashCode, length, MajorVersionNumber, MinorVersionNumber, SerialNumber);
                map.setInfo(info);
            }
            return map;
        }
        if (isValid()) {
            ByteArrayOutputStream dataString = new ByteArrayOutputStream();
            List<UDPByteDataInfo> dataByte = new ArrayList<>();
            // 对数据进行分类处理
            for (int i = 0; i < length; i++) {
                UDPDataInfo info = infos[i];
                if (time != info.time || code != info.code || hashCode != info.hashCode) {
                    throw new Exception("数据包在 " + i + "发生了不匹配,当前包信息为:" + this + ",待合并包信息为 " + info);
                } else {
                    int type = info.data[0] >>> 7 & 0x01;
                    byte[] data = info.data;
                    if (type == 0) {
                        dataString.write(Arrays.copyOfRange(data, 1, data.length));
                    } else {
                        int keylength = data[0] & 0x7f;
                        int length = (data[1] & 0xff) << 8 | (data[2] & 0xff);
                        int num = (data[3] & 0xff) << 8 | (data[4] & 0xff);
                        String key = new String(data, 5, keylength, ENCODE.getValue());
                        dataByte.add(new UDPByteDataInfo(Arrays.copyOfRange(data, 5 + keylength, data.length),
                                length, num, key));
                    }
                }
            }
            JSONObject object = new JSONObject(new String(dataString.toByteArray(), ENCODE.getValue()));
            for (String key : object.keySet()) {
                if (key.equals("JSONS")) {
                    JSONObject jsonObject = new JSONObject(object.getString(key));
                    for (String jsonkey : jsonObject.keySet()) {
                        map.put(jsonkey, jsonObject.getJSONObject(jsonkey));
                    }
                } else {
                    map.put(key, object.optString(key));
                }
            }
            dataString.close();
            UDPByteDataInfo lastInfo = null;
            for (UDPByteDataInfo byteInfo : dataByte) {
                // System.out.println(byteInfo);
                if (lastInfo == null) {
                    lastInfo = byteInfo;
                    lastInfo.init();
                    if (lastInfo.isValid()) {
                        lastInfo.getData(map);
                        lastInfo = null;
                    }
                    continue;
                }
                lastInfo.add(byteInfo);
                if (lastInfo.isValid()) {
                    lastInfo.getData(map);
                    lastInfo = null;
                }

            }
        }
        {
            UDPDataInfo info = new UDPDataInfo(ENCODE, time, startTime, lastTime, System.currentTimeMillis(), datasize,
                    code, hashCode, length, MajorVersionNumber, MinorVersionNumber, SerialNumber);
            map.setInfo(info);
        }
        return map;
    }

    // 添加当前包其他数据
    protected void put(UDPDataInfo info) {
        if (infos == null) {
            infos = new UDPDataInfo[length];
            value = new byte[(int) Math.ceil((double) length / 8)];
            value[num / 8] |= 1 << (num % 8);
            infos[num] = this;
        }
        comst++;
        int n = info.num;
        value[n / 8] |= (1 << (n % 8));
        if (infos[n] != null) {
            System.err.println("位置 " + n + " 发送覆盖");
        }
        startTime = Math.min(startTime, info.startTime);
        lastTime = Math.max(lastTime, info.lastTime);
        infos[n] = info;
    }

    protected boolean isTimeOut(long allTimeOut, long lastTimeOut) {
        long nowTime = System.currentTimeMillis();
        return ((startTime + allTimeOut) < nowTime) || ((lastTime + lastTimeOut) < nowTime);
    }

    public long getSendTime() {
        return time;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastTime() {
        return lastTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public FileRW getEnCode() {
        return ENCODE;
    }

    public int getDataSize() {
        return datasize;
    }

    public String getGiatus() {
        return length + ":" + Arrays.toString(value);
    }

    public String getDataInfo() {
        return "DataInfo [编码=" + ENCODE.getValue() + ", 发送时间=" + time + ", 首收时间=" + startTime + ", 最近收取时间="
                + lastTime + ",处理结束时间=" + endTime + ", 缓存区大小=" + datasize + ", 随机码=" + code + ", 数据码=" + hashCode
                + ", 数据包总长=" + length + ", 版本号=" + MajorVersionNumber + "." + MinorVersionNumber + "." + SerialNumber
                + "]";
    }

    @Override
    public String toString() {
        return "UDPDataInfo [编码=" + ENCODE.getValue() + ", 发送时间=" + time + ", 首收时间=" + startTime + ", 最近收取时间="
                + lastTime + ",处理结束时间=" + endTime + ", 缓存区大小=" + datasize + ", 随机码=" + code + ", 数据码=" + hashCode
                + ", 数据包总长=" + length + ", 数据包序号=" + num + ", 版本号=" + MajorVersionNumber + "."
                + MinorVersionNumber + "." + SerialNumber + "]";
    }
}
