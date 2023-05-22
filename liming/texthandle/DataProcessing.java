package liming.texthandle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

/**
 * 数据处理类，封装处理流程
 */
class DataProcessing {
    private static final int UUID = java.util.UUID.randomUUID().toString().hashCode();
    private static final int MajorVersionNumber = 3;// 主版本号 协议主要版本 暂时各主版本不兼容
    private static final int MinorVersionNumber = 0;// 副版本号 主要在性能方向的优化版本
    private static final int SerialNumber = 1;// 序列号 测试时的序列号

    private static final byte STRINGBYTE = (byte) 0x00, BYTEBYTE = (byte) 0x8f;// 高位1为byte数据，0为String数据

    private static final byte[] VERSION = new byte[] { MajorVersionNumber, MinorVersionNumber, SerialNumber };// 版本号

    protected static final String getVesion() {
        return "版本号=" + MajorVersionNumber + "." + MinorVersionNumber + "." + SerialNumber;
    }

    protected static List<DatagramPacket> getPackets(FileRW ENCODED, int DATASIZE, ReceiveMap map) throws Exception {
        // System.out.println(map);
        byte[] info = getInfo(ENCODED, DATASIZE, System.currentTimeMillis(),
                new Random(System.currentTimeMillis() / UUID + UUID).nextInt(899)
                        + 100,
                map.hashCode());

        List<byte[]> datasString = putStringData(ENCODED, DATASIZE,
                new JSONObject(map.getString()).toString().getBytes(ENCODED.getValue()));
        List<byte[]> datasByte = new ArrayList<>();
        {
            // 获取byte类型的消息
            Map<String, byte[]> bytes = map.getByte();
            for (String key : bytes.keySet()) {
                datasByte.addAll(putByteData(ENCODED, DATASIZE, key, bytes.get(key)));
            }
        }

        List<byte[]> dataAll = new ArrayList<>();
        dataAll.addAll(datasString);
        dataAll.addAll(datasByte);
        info = putData(info, 11, 12, dataAll.size());
        List<DatagramPacket> packages = new ArrayList<>();
        int num = 0;
        for (byte[] temp : dataAll) {
            byte[] t = new byte[temp.length + info.length];
            System.arraycopy(info, 0, t, 0, info.length);
            System.arraycopy(temp, 0, t, info.length, temp.length);
            t = putData(t, 13, 14, num++);
            packages.add(new DatagramPacket(t, t.length));
        }
        return packages;
    }

    /* 将时间、随机数和hashCode写入和基本信息写入数据头 */
    private static byte[] getInfo(FileRW ENCODED, int DATASIZE, long time, int code, int hashCode) {
        System.out.println(time + " " + code + " " + hashCode);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            time = time & 0x3ffffffffffl;
            code = code & 0x3ff;
            outputStream.write((ENCODED.getNum() << 4 & 0xf0) | ((int) (time >>> 38 & 0x0f)));// 0
            outputStream.write((int) ((time >>> 30) & 0Xff));// 1
            outputStream.write((int) (time >>> 22) & 0Xff);// 2
            outputStream.write((int) (time >>> 14) & 0Xff);// 3
            outputStream.write((int) (time >>> 6) & 0Xff);// 4
            outputStream.write((int) (time << 2 & 0Xfc | code >>> 8 & 0x03));// 5
            outputStream.write(code & 0xff);// 6
            outputStream.write(hashCode >> 24 & 0xff);// 7
            outputStream.write(hashCode >> 16 & 0xff);// 8
            outputStream.write(hashCode >> 8 & 0xff);// 9
            outputStream.write(hashCode & 0xff);// 10
            // 11 12为数据总长 13 14为数据编号
            outputStream.write(0); // 11
            outputStream.write(0); // 12
            outputStream.write(0); // 13
            outputStream.write(0); // 14

            outputStream.write(DATASIZE >> 8 & 0xff);// 15
            outputStream.write(DATASIZE & 0xff);// 16
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

    private static List<byte[]> putStringData(FileRW ENCODED, int DATASIZE, byte[] bytes)
            throws UnsupportedEncodingException {
        List<byte[]> datasString = new ArrayList<>();
        int length = bytes.length;
        int size = DATASIZE - 20 - 1;// 当前包的大小除开20位标识符和版本号后的大小
        for (int i = 0; i < length;) {
            byte[] data;
            if (length - i < size)
                data = Arrays.copyOfRange(bytes, i, i = length);
            else
                data = Arrays.copyOfRange(bytes, i, i = i + size);
            byte[] newData = new byte[data.length + 1];
            newData[0] = STRINGBYTE;
            System.arraycopy(data, 0, newData, 1, data.length);
            datasString.add(newData);
        }
        return datasString;
    }

    private static List<byte[]> putByteData(FileRW ENCODED, int DATASIZE, String key, byte[] bytes) throws Exception {
        byte[] keyB = key.getBytes(ENCODED.getValue());
        int Length = keyB.length;// 键所用长度
        if ((Length & 0xfffff80) > 0)
            throw new Exception("对byte数据中key超过规定长度，请检查编码格式和key长度");
        byte[] hand = null;
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((BYTEBYTE & 0x80) | (Length & 0x7f));// 0
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
                data = Arrays.copyOfRange(bytes, i, i = length);
            } else {
                data = Arrays.copyOfRange(bytes, i, i = i + size);
            }
            byte[] newData = new byte[data.length + hand.length];
            System.arraycopy(data, 0, newData, hand.length, data.length);
            datasByte.add(newData);
        }
        hand = putData(hand, 1, 2, datasByte.size());
        List<byte[]> list = new ArrayList<>();
        int num = 0;
        for (byte[] temp : datasByte) {
            System.arraycopy(hand, 0, temp, 0, hand.length);
            list.add(putData(temp, 3, 4, num++));
        }
        return list;
    }

    private static byte[] putData(byte[] info, int index1, int index2, int num) {
        int num1 = num >> 8 & 0xFF;
        int num2 = num & 0xFF;
        info[index1] = (byte) num1;
        info[index2] = (byte) num2;
        return info;
    }
}
