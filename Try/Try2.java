package Try;

import java.io.EOFException;

import liming.key.encryption.RSA_Encryption;
import liming.texthandle.FileRW;

public class Try2 {
    public static void main(String[] args) throws EOFException {
        new Try2();
    }

    public Try2() throws EOFException {
        long time = System.currentTimeMillis();
        byte[] infobs = getInfo(time, 992, 123123);
        infobs = putDataLength(infobs, 12345);
        infobs = putDataNum(infobs, 123);
        Info info = new Info(infobs);
        System.out.println(info);
        System.out.println(time);
    }

    static class M2 {
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

    class Info {
        int encode;
        long time;// 数据 时间
        int datasize;// 缓冲区长度
        int code;// 随机码
        int hashCode;
        int length;
        int num;

        public Info(byte[] info) throws EOFException {
            if (info.length < 17) {
                throw new EOFException("非本协议的数据包,终止处理,其中Bast64的结果为: " + RSA_Encryption.signatureToString(info));
            }
            encode = info[0] / M2.get(4);

            int[] ints = new int[17];
            for (int i = 0; i < 17; i++) {
                ints[i] = valueOf(info[i]);
            }

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
        }

        @Override
        public String toString() {
            return "Info [encode=" + encode + ", time=" + time + ", datasize=" + datasize + ", code=" + code
                    + ", hashCode=" + hashCode + ", length=" + length + ", num=" + num + "]";
        }

    }

    static FileRW ENCODED = FileRW.GBK;
    static int DATASIZE = 1024;

    private static int valueOf(byte b) {
        return b < 0 ? (256 + b) : b;
    }

    public static byte[] getInfo(long time, int code, int hashCode) {
        byte[] info = new byte[17];
        info[0] = (byte) (ENCODED.getNum() * M2.get(4));
        time = time % M2.getLong(42);
        code = code % M2.get(10);
        info[0] = (byte) (valueOf(info[0]) + (time / M2.getLong(38)));
        info[1] = (byte) (time % M2.getLong(38) / M2.getLong(30));
        info[2] = (byte) (time % M2.getLong(30) / M2.getLong(22));
        info[3] = (byte) (time % M2.getLong(22) / M2.getLong(14));
        info[4] = (byte) (time % M2.getLong(14) / M2.getLong(6));
        info[5] = (byte) (time % M2.getLong(6) * M2.get(2));
        info[5] = (byte) (valueOf(info[5]) + code / M2.get(8));
        info[6] = (byte) (code % M2.get(8));
        info[7] = (byte) (hashCode / M2.get(24));
        info[8] = (byte) (hashCode % M2.get(24) / M2.get(16));
        info[9] = (byte) (hashCode % M2.get(16) / M2.get(8));
        info[10] = (byte) (hashCode % M2.get(8));
        info[15] = (byte) (DATASIZE % M2.get(16) / M2.get(8));
        info[16] = (byte) (DATASIZE / M2.get(8));
        return info;
    }

    public static synchronized byte[] putDataLength(byte[] info, int length) {
        info[11] = (byte) (length % M2.get(16) / M2.get(8));
        info[12] = (byte) (length % M2.get(8));
        return info;
    }

    public static synchronized byte[] putDataNum(byte[] info, int num) {
        info[13] = (byte) (num % M2.get(16) / M2.get(8));
        info[14] = (byte) (num % M2.get(8));
        return info;
    }
}
