import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import liming.key.encryption.RSA_Encryption;
import liming.texthandle.GetDataAndPacket;
import liming.texthandle.HandleReceive;
import liming.texthandle.IO;

public class S {
    public String picture;
    IO.IOScreen ioscreen;
    long rn = 0;

    ScreenCapture screenCapture;

    private float quality = 0.1f;// 设置图片的压缩比
    private String code = "jpg";// 设置图片格式
    private float factor = 0.2f;// 获取时为避免线程过多性能衰减，设置在同时运行的线程数，占帧率的百分比
    private int frame = 15;// 帧率
    private float buffer = 0f;// 与一秒比较，缓冲的大小

    public static void main(String[] args) {
        try {
            new S();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public S() throws SocketException {

        screenCapture = new ScreenCapture(quality, code, factor, frame, buffer);
        screenCapture.start();
        long gap = screenCapture.getGap();
        // HandleReceive.setDebug(true);

        receive = new HandleReceive(new GetDataAndPacket() {

            @Override
            public void sendDataToClient(Map<String, String> data, InetAddress address, int port,
                    DatagramSocket socket) {

                System.out.println("获取数据: " + data);
                rn++;
                int sleep;
                long now_time;
                try {
                    sleep = Integer.parseInt(data.get("sleep"));
                } catch (Exception e) {
                    sleep = 0;
                }
                try {
                    now_time = Long.parseLong(data.get("now"));
                } catch (Exception e) {
                    now_time = System.currentTimeMillis();
                }
                System.out.println(now_time + "  " + sleep);
                if (sleep >= (1 + buffer) * 1000 - 2 * gap)
                    sleep = 500;
                if (sleep <= gap)
                    sleep = (int) gap + 20;
                long now = now_time;
                long gettime = sleep;
                System.out.println(now + "  " + gettime);
                new Thread(() -> {
                    List<byte[]> bytes = null;
                    while (bytes == null || bytes.size() <= 1) {
                        bytes = screenCapture.get(now, gettime);
                    }
                    System.out.println("处理发送中 " + bytes.size());
                    Map<String, String> d = new HashMap<>();
                    int i = 0;
                    for (byte[] b : bytes) {
                        i++;
                        if (b == null) {
                            d.put("p_" + i, "null");
                        } else {
                            try {
                                String p = RSA_Encryption.signatureToString(b);
                                d.put("p_" + i, p);
                            } catch (Exception e) {
                                d.put("p_" + i, "null");
                            }
                        }
                    }
                    d.put("now", now + "");
                    d.put("size", bytes.size() + "");
                    try {
                        receive.send(d, address, port, socket);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    // receive.stop();
                    // screenCapture.stop();
                }).start();

            }

            @Override
            public void writeLog(Object message) {
                System.out.println("writeLog: " + message);
            }

            @Override
            public void writeStrongLog(Object message) {
                System.out.println("writeStrongLog: " + message);
            }

            @Override
            public boolean isDataReceived(long timeout) {
                return false;
            }

        }, 6465, 512);
        receive.start();
    }

    HandleReceive receive;
}
