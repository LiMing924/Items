import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import liming.texthandle.HandleReceive;
import liming.texthandle.IO;
import liming.texthandle.ReceiveMap;

public class S {
    public String picture;
    IO.IOScreen ioscreen;
    long rn = 0;

    ScreenCapture screenCapture;

    private float quality = 0.5f;// 设置图片的压缩比
    private String code = "jpg";// 设置图片格式
    private float factor = 0.2f;// 获取时为避免线程过多性能衰减，设置在同时运行的线程数，占帧率的百分比
    private int frame = 24;// 帧率
    private float buffer = 1f;// 与一秒比较，缓冲的大小

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

        receive = new HandleReceive(6465, 20480) {

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

            @Override
            public void sendDataToClient(ReceiveMap data, InetAddress address, int port, DatagramSocket socket) {
                System.out.println("网络延时:" + (data.getReceiveTime() - data.getSendTime()));
                System.out.println("处理延时:" + (data.getLastTime() - data.getReceiveTime()));
                System.out.println("获取数据: " + data);
                long sTime = System.currentTimeMillis();// 服务器接收完数据的时间
                rn++;
                int sleep_time;// 客户端请求的时间片间隔
                long start_time;// 客户端请求的时间起点
                try {
                    sleep_time = Integer.parseInt(data.getString("sleep"));
                } catch (Exception e) {
                    sleep_time = 0;
                }
                try {
                    start_time = Long.parseLong(data.getString("start"));
                } catch (Exception e) {
                    start_time = System.currentTimeMillis();
                }
                System.out.println(start_time + "  " + sleep_time);
                if (sleep_time >= (1 + buffer) * 1000 - 2 * gap)
                    sleep_time = 500;
                if (sleep_time <= gap)
                    sleep_time = (int) gap + 20;
                long start = start_time;
                long sleep = sleep_time;
                System.out.println(start + "  " + sleep);
                new Thread(() -> {
                    long hTime = System.currentTimeMillis();
                    List<byte[]> bytes = null;
                    while (bytes == null || bytes.size() <= 1) {
                        bytes = screenCapture.getByte(start, sleep);
                    }
                    System.out.println("处理发送中 " + bytes.size());
                    ReceiveMap map = new ReceiveMap();
                    int i = 0;
                    for (byte[] b : bytes) {
                        i++;
                        if (b == null) {
                            map.put("p_" + i, new byte[0]);
                        } else {
                            try {
                                map.put("p_" + i, b);
                            } catch (Exception e) {
                                map.put("p_" + i, new byte[0]);
                                System.out.println("p_" + i + " null");
                            }
                        }
                    }

                    map.put("size", bytes.size() + "");// 回复图片范围
                    map.put("frame", frame + "");// 回复服务器帧率
                    map.put("code", code);// 回复服务器图片编码格式
                    map.put("satrt", start + "");// 回复时间起点
                    map.put("sTime", sTime + "");// 回复服务器完整接收到数据的时间
                    map.put("hTime", hTime + "");// 回复服务器开始处理图片数据的时间
                    map.put("sEnd", System.currentTimeMillis() + "");// 回复服务器结束处理的时间
                    System.out.println(map);
                    try {
                        receive.send(map, address, port, socket);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

        };
        receive.start();
    }

    HandleReceive receive;
}
