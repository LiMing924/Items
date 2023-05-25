import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import liming.texthandle.HandleReceive;
import liming.texthandle.ReceiveMap;

public class C {
    HandleReceive receive;

    public static void main(String[] args) throws SocketException, UnknownHostException {
        new C();
    }

    public C() throws SocketException, UnknownHostException {
        // HandleReceive.setDebug(true);
        receive = new HandleReceive(6666, 20480) {
            @Override
            public void sendDataToClient(ReceiveMap data, InetAddress address, int port,
                    DatagramSocket socket) {
                System.out.println("发送时间:" + data.getSendTime());
                System.out.println("首收时间:" + data.getReceiveTime());
                System.out.println("结束时间:" + data.getLastTime());
                System.out.println("网络延时:" + (data.getReceiveTime() - data.getSendTime()));
                System.out.println("处理延时:" + (data.getLastTime() - data.getReceiveTime()));
                System.out.println(data);
                long cTime = System.currentTimeMillis();// 客户端收到完整数据的时间
                int size = Integer.parseInt(data.getString("size"));

                long start = Long.valueOf(data.getString("satrt"));
                long sTime = Long.valueOf(data.getString("sTime"));
                long shTime = Long.valueOf(data.getString("hTime"));
                long sEnd = Long.valueOf(data.getString("sEnd"));

                long chTime = System.currentTimeMillis();

                {
                    File file = new File("./img/");
                    file.mkdir();
                }

                BufferedImage bufferImage = null;
                for (int i = 1; i <= size; i++) {
                    System.out.println("处理照片 " + i);
                    byte[] p = data.getByte("p_" + i);
                    if (p == null || p.length == 0) {
                        System.out.println("处理照片 " + i + " 数据无效");
                        continue;
                    }
                    ByteArrayInputStream bis = new ByteArrayInputStream(p);
                    BufferedImage image;
                    try {
                        image = ImageIO.read(bis);
                        bis.close();
                        // 创建双缓冲区
                        if (bufferImage == null) {
                            bufferImage = new BufferedImage(image.getWidth(), image.getHeight(),
                                    BufferedImage.TYPE_INT_RGB);
                        }
                        // 在缓冲区中绘制图像
                        Graphics2D g2d = bufferImage.createGraphics();
                        g2d.drawImage(image, 0, 0, null);
                        g2d.dispose();

                        // 将图像保存在本地
                        File outputfile = new File("./img/image" + i + ".jpg");
                        try {
                            ImageIO.write(bufferImage, "jpg", outputfile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                long time = System.currentTimeMillis();
                receive.stop();
                System.out.println("时间起点:               \t" + start);
                System.out.println("服务器完整接收数据时间:  \t" + sTime + " \tC->S时间:               \t" + (sTime - start));
                System.out.println("服务器开始处理数据时间:  \t" + shTime + " \t服务器启动回发线程时间: \t" + (shTime - sTime));
                System.out.println("服务器结束处理数据时间:  \t" + sEnd + " \t服务器处理图片数据时间:   \t" + (sEnd - shTime));
                System.out.println("客户端完整接收数据时间:  \t" + cTime + " \tS-C时间:                \t" + (cTime - sEnd));
                System.out.println("客户端开始处理数据时间:  \t" + chTime + " \t客户端接收标识符时间:   \t " + (chTime - cTime));
                System.out.println("客户端结束处理数据时间:  \t" + time + " \t客户端处理数据时间:       \t" + (time - chTime));
                System.out.println("总用时: " + (time - start) + " mm");
            }

            @Override
            public boolean isDataReceived(long timeout) {
                return false;
            }
        };
        receive.start();
        ReceiveMap map = new ReceiveMap();
        map.put("sleep", "1000");
        map.put("start", System.currentTimeMillis() + "");
        // while (true) {
        try {
            // receive.send(d, "45.125.46.48", 11378);
            receive.send(map, "127.0.0.1", 6465);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
