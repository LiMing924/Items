package Try;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import liming.key.encryption.RSA_Encryption;
import liming.texthandle.FileRW;
import liming.texthandle.ReceiveMap;

public class Try3 {
    // 基本变量
    static FileRW ENCODED = FileRW.GBK;
    static int DATASIZE = 1024;
    int sendnum = 0;

    private static final int MajorVersionNumber = 3;// 主版本号 协议主要版本 暂时各主版本不兼容
    private static final int MinorVersionNumber = 0;// 副版本号 主要在性能方向的优化版本
    private static final int SerialNumber = 1;// 序列号 测试时的序列号

    private static final byte STRINGBYTE = (byte) (0 * M2.get(7)), BYTEBYTE = (byte) (1 * M2.get(7));// 高位1为byte数据，0为String数据

    private static final byte[] VERSION = new byte[] { MajorVersionNumber, MinorVersionNumber, SerialNumber };// 版本号

    public static void main(String[] args) throws Exception {

        ReceiveMap map = new ReceiveMap();

        map.put("ks1", "v1");
        map.put("ks2", "v2");
        map.put("ks3", "v3");
        map.put("kd1", "v1".getBytes(ENCODED.getValue()));
        map.put("kd2", "v2".getBytes(ENCODED.getValue()));
        map.put("kd3", "v3".getBytes(ENCODED.getValue()));
        for (String key : map.getStringKey()) {
            System.out.println(key + ":" + map.optString(key));
        }
        for (String key : map.getByteKey()) {
            System.out.println(key + ":" + Arrays.toString(map.optByte(key)));
        }
        List<DatagramPacket> list = new Try3().getPackets(map);
        Info info = new Info(list.get(0).getData());
        for (int i = 1; i < list.size(); i++) {
            info.put(new Info(list.get(i)));
        }
        ReceiveMap map2 = info.getReceiveMap();
        System.out.println(map2.getStringKey());
        System.out.println(map2);
        System.out.println(info);
        for (String key : map.getStringKey()) {
            System.out.println(key + ":" + map.optString(key));
        }
        for (String key : map.getByteKey()) {
            System.out.println(key + ":" + Arrays.toString(map.optByte(key)));
        }
        for (String key : map2.getStringKey()) {
            System.out.println(key + ":" + map2.optString(key));
        }
        for (String key : map2.getByteKey()) {
            System.out.println(key + ":" + Arrays.toString(map2.optByte(key)));
        }
    }

    static class Info {
        private FileRW ENCODE;// 编码
        private long time;// 数据 时间
        private int datasize;// 缓冲区长度
        private int code;// 随机码
        private int hashCode;
        private int length;// 数据总长
        private int num;// 当前长度
        private int MajorVersionNumber;// 主版本号
        private int MinorVersionNumber;// 副版本号
        private int SerialNumber;// 序列号

        private byte[] data;// 这个包携带的数据

        public Info(DatagramPacket packet) throws Exception {
            this(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
        }

        public Info(byte[] info) throws Exception {
            if (info.length < 20) {
                throw new Exception("非本协议的数据包,终止处理,其中Bast64的结果为: " + RSA_Encryption.signatureToString(info));
            }
            int[] ints = new int[20];
            for (int i = 0; i < 20; i++) {
                ints[i] = valueOf(info[i]);
            }
            ENCODE = FileRW.getFileRW(ints[0] / M2.get(4));
            time = (long) ints[0] % M2.get(4) * M2.getLong(38);
            time += (long) ints[1] * M2.getLong(30);
            time += (long) ints[2] * M2.get(22);
            time += (long) ints[3] * M2.get(14);
            time += (long) ints[4] * M2.get(6);
            time += (long) (ints[5] / M2.get(2));

            code = ints[5] % M2.get(2) * M2.get(8);
            code += ints[6];

            hashCode = ints[7] * M2.get(24);
            hashCode += ints[8] * M2.get(16);
            hashCode += ints[9] * M2.get(8);
            hashCode += ints[10];
            length = ints[11] * M2.get(8);
            length += ints[12];
            num = ints[13] * M2.get(8);
            num += ints[14];
            datasize = ints[15] * M2.get(8);
            datasize += ints[16];
            MajorVersionNumber = ints[17];
            MinorVersionNumber = ints[18];
            SerialNumber = ints[19];
            data = Arrays.copyOfRange(info, 20, info.length);
        }

        // 得到包标识
        public String getID() {
            return time + "-" + code + "-" + hashCode;
        }

        private int comst = 1;
        private Info[] infos;

        public boolean isValid() {
            if (comst != length || infos == null)
                return false;
            for (int i = 0; i < length; i++) {
                if (infos[i] == null)
                    return false;
            }
            return true;
        }

        public ReceiveMap getReceiveMap() throws Exception {
            ReceiveMap map = new ReceiveMap();
            if (isValid()) {
                ByteArrayOutputStream dataString = new ByteArrayOutputStream();
                List<ByteInfo> dataByte = new ArrayList<>();
                // 对数据进行分类处理
                for (int i = 0; i < length; i++) {
                    Info info = infos[i];
                    if (time != info.time || code != info.code || hashCode != info.hashCode) {
                        throw new Exception("数据包在 " + i + "发生了不匹配,当前包信息为:" + this + ",待合并包信息为 " + info);
                    } else {
                        int type = valueOf(info.data[0]) / M2.get(7);
                        byte[] data = info.data;
                        if (type == 0) {
                            dataString.write(Arrays.copyOfRange(data, 1, data.length));
                        } else {
                            int keylength = valueOf(data[0]) % M2.get(7);
                            int length = valueOf(data[1]) * M2.get(8) + valueOf(data[2]);
                            int num = valueOf(data[3]) * M2.get(8) + valueOf(data[4]);
                            String key = new String(data, 5, keylength, ENCODE.getValue());
                            dataByte.add(new ByteInfo(Arrays.copyOfRange(data, 5 + keylength, data.length),
                                    length, num, key));
                        }
                    }
                }
                JSONObject object = new JSONObject(new String(dataString.toByteArray(), ENCODE.getValue()));
                for (String key : object.keySet()) {
                    map.put(key, object.optString(key));
                }
                dataString.close();
                ByteInfo lastInfo = null;
                for (ByteInfo byteInfo : dataByte) {
                    System.out.println(byteInfo);
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
            return map;
        }

        private class ByteInfo {
            byte[] data;// 包内有效数据
            int length;// 包总长
            int num;// 包序号
            String key;// 包key
            ByteInfo[] byteInfos;// 缓冲队列
            int comst = 1;// 计数器

            ByteInfo(byte[] data, int length, int num, String key) {
                this.data = data;
                this.length = length;
                this.num = num;
                this.key = key;
            }

            void init() {
                if (byteInfos == null) {
                    byteInfos = new ByteInfo[length];
                    byteInfos[num] = this;
                }
            }

            void add(ByteInfo info) {
                comst++;
                byteInfos[info.num] = info;
            }

            boolean isValid() {
                if (comst < length || byteInfos == null) {
                    return false;
                }
                for (int i = length - 1; i >= 0; i--) {
                    if (byteInfos[i] == null)
                        return false;
                }
                return true;
            }

            void getData(ReceiveMap map) throws IOException {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (int i = 0; i < length; i++) {
                    outputStream.write(byteInfos[i].data);
                }
                map.put(key, outputStream.toByteArray());
                outputStream.close();
            }

            @Override
            public String toString() {
                return "ByteInfo [data=" + Arrays.toString(data) + ", length=" + length + ", num=" + num + ", key="
                        + key + "]";
            }

        }

        // 添加当前包其他数据
        public void put(Info info) {
            if (infos == null) {
                infos = new Info[length];
                infos[num] = this;
            }
            comst++;
            int n = info.num;
            if (infos[n] != null) {
                System.err.println("位置 " + n + " 发送覆盖");
            }
            infos[n] = info;
        }

        @Override
        public String toString() {
            return "Info [编码=" + ENCODE.getValue() + ", 时间戳=" + time + ", 缓存区大小=" + datasize + ", 随机码=" + code
                    + ", 数据码=" + hashCode + ", 数据包总长=" + length + ", 数据包序号=" + num + ", 版本号="
                    + MajorVersionNumber + "." + MinorVersionNumber + "."
                    + SerialNumber + "]";
        }

    }

    public List<DatagramPacket> getPackets(ReceiveMap receiveMap) throws Exception {
        sendnum++;
        // 获取数据包基本信息 编码格式，数据ID
        byte[] info = getInfo(System.currentTimeMillis(),
                new Random(System.currentTimeMillis() + receiveMap.hashCode() / 2).nextInt(899)
                        + 100,
                receiveMap.hashCode());

        List<byte[]> datasString = putStringData(
                new JSONObject(receiveMap.getString()).toString().getBytes(ENCODED.getValue()));
        List<byte[]> datasByte = new ArrayList<>();
        {
            // 获取byte类型的消息
            Map<String, byte[]> bytes = receiveMap.getByte();
            for (String key : bytes.keySet()) {
                datasByte.addAll(putByteData(key, bytes.get(key)));
            }
        }

        List<byte[]> dataAll = new ArrayList<>();
        dataAll.addAll(datasString);
        dataAll.addAll(datasByte);
        info = putDataLength(info, 11, 12, dataAll.size());
        List<DatagramPacket> packages = new ArrayList<>();
        int num = 0;
        for (byte[] temp : dataAll) {
            byte[] t = new byte[temp.length + info.length];
            System.arraycopy(info, 0, t, 0, info.length);
            System.arraycopy(temp, 0, t, info.length, temp.length);
            t = putDataNum(t, 13, 14, num++);
            packages.add(new DatagramPacket(t, t.length));
        }
        return packages;
    }

    /** 在long和int范围内2的指数值 */
    private static class M2 {
        private static long[] m2;
        static {
            m2 = new long[64];
            for (int i = 0; i < 64; i++) {
                m2[i] = 1;
            }
            for (int i = 1; i < 64; i++) {
                for (int j = i; j < 64; j++) {
                    m2[j] = m2[j] * 2;
                }
            }
        }

        public static int get(int i) {
            return (int) m2[i];
        }

        public static long getLong(int i) {
            return m2[i];
        }
    }

    /* 将时间、随机数和hashCode写入和基本信息写入数据头 */
    private byte[] getInfo(long time, int code, int hashCode) {
        // System.out.println(time + " " + code + " " + hashCode);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            time = time % M2.getLong(42);
            code = code % M2.get(10);
            // if (hashCode < 0)
            // hashCode = hashCode * (-1);//hashCode取正
            // System.out.println(time + " " + code + " " + hashCode);
            outputStream.write(ENCODED.getNum() * M2.get(4) + (int) (time / M2.getLong(38)));// 0
            outputStream.write((int) (time % M2.getLong(38) / M2.getLong(30)));// 1
            outputStream.write((int) (time % M2.getLong(30) / M2.getLong(22)));// 2
            outputStream.write((int) (time % M2.getLong(22) / M2.getLong(14)));// 3
            outputStream.write((int) (time % M2.getLong(14) / M2.getLong(6)));// 4
            outputStream.write((int) (time % M2.getLong(6) * M2.get(2)) + code / M2.get(8));// 5
            outputStream.write(code % M2.get(8));// 6
            outputStream.write(hashCode / M2.get(24));// 7
            outputStream.write(hashCode % M2.get(24) / M2.get(16));// 8
            outputStream.write(hashCode % M2.get(16) / M2.get(8));// 9
            outputStream.write(hashCode % M2.get(8));// 10

            outputStream.write(0); // 11
            outputStream.write(0); // 12
            outputStream.write(0); // 13
            outputStream.write(0); // 14

            outputStream.write(DATASIZE % M2.get(16) / M2.get(8));// 15
            outputStream.write(DATASIZE % M2.get(8));// 16
            outputStream.write(VERSION[0]);// 17
            outputStream.write(VERSION[1]);// 18
            outputStream.write(VERSION[2]);// 19

            return outputStream.toByteArray();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] putDataLength(byte[] info, int index1, int index2, int length) {
        info[index1] = (byte) (length % M2.get(16) / M2.get(8));
        info[index2] = (byte) (length % M2.get(8));
        return info;
    }

    private static byte[] putDataNum(byte[] info, int index1, int index2, int num) {
        info[index1] = (byte) (num % M2.get(16) / M2.get(8));
        info[index2] = (byte) (num % M2.get(8));
        return info;
    }

    private List<byte[]> putStringData(byte[] bytes) throws UnsupportedEncodingException {
        List<byte[]> datasString = new ArrayList<>();
        int length = bytes.length;
        int size = DATASIZE - 20 - 1;// 当前包的大小除开20位标识符和版本号后的大小
        for (int i = 0; i < length;) {
            byte[] data;
            if (length - i < size) {
                data = Arrays.copyOfRange(bytes, i, length);
                i = length;
            } else {
                data = Arrays.copyOfRange(bytes, i, size);
                i = i + size;
            }
            byte[] newData = new byte[data.length + 1];
            newData[0] = STRINGBYTE;
            System.arraycopy(data, 0, newData, 1, data.length);
            datasString.add(newData);
        }
        return datasString;
    }

    private List<byte[]> putByteData(String key, byte[] bytes) throws Exception {
        byte[] keyB = key.getBytes(ENCODED.getValue());
        int Length = keyB.length;// 键所用长度
        if (Length >= M2.get(7))
            throw new EOFException("对byte数据中key超过规定长度，请检查编码格式和key长度");
        byte[] hand = null;
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(valueOf(BYTEBYTE) + Length);// 0
            outputStream.write(0);// 1
            outputStream.write(0);// 2
            outputStream.write(0);// 3
            outputStream.write(0);// 4
            outputStream.write(keyB);// 键的二进制编码
            hand = outputStream.toByteArray();
            outputStream.close();
        }
        List<byte[]> datasByte = new ArrayList<>();
        int length = bytes.length;// 待发送数据总长
        int size = DATASIZE - 20 - hand.length;// 抛开标识后可用的长度
        for (int i = 0; i < length;) {
            byte[] data;
            if (length - i < size) {
                data = Arrays.copyOfRange(bytes, i, length);
                i = length;
            } else {
                data = Arrays.copyOfRange(bytes, i, size);
                i = i + size;
            }
            byte[] newData = new byte[data.length + hand.length];
            newData[0] = BYTEBYTE;
            System.arraycopy(data, 0, newData, hand.length, data.length);
            datasByte.add(newData);
        }
        hand = putDataLength(hand, 1, 2, datasByte.size());
        int num = 0;
        for (byte[] temp : datasByte) {
            System.arraycopy(hand, 0, temp, 0, hand.length);
            temp = putDataNum(temp, 3, 4, num++);
        }
        return datasByte;
    }

    private static int valueOf(byte b) {
        return b < 0 ? (256 + b) : b;
    }
}
