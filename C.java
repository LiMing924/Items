import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import liming.key.encryption.RSA_Encryption;
import liming.texthandle.GetDataAndPacket;
import liming.texthandle.HandleReceive;

public class C {
    HandleReceive receive;

    public static void main(String[] args) throws SocketException {
        new C();
    }

    public C() throws SocketException {
        // HandleReceive.setDebug(true);
        receive = new HandleReceive(new GetDataAndPacket() {

            @Override
            public void sendDataToClient(Map<String, String> data, InetAddress address, int port,
                    DatagramSocket socket) {
                System.out.println(data);
                int size = Integer.parseInt(data.get("size"));

                BufferedImage bufferImage = null;
                for (int i = 0; i < size; i++) {
                    System.out.println("处理照片 " + i);
                    String p = data.get("p_" + i);
                    if (p == null || p.length() <= 4 || p.toUpperCase().equals("NULL")) {
                        System.out.println("处理照片 " + i + " 数据无效");
                        continue;
                    }
                    byte[] d = RSA_Encryption.stringToSignature(p);
                    if (d == null || d.length == 0) {
                        System.out.println("处理照片 " + i + " 二进制数据处理无效");
                        continue;
                    }
                    ByteArrayInputStream bis = new ByteArrayInputStream(d);
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
                receive.stop();
            }

            @Override
            public void writeLog(Object message) {
                if (receive.getDebug())
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

        }, 6666, 512);
        receive.start();
        receive.start();

        Map<String, String> d = new HashMap<>();
        d.put("sleep", "800");
        d.put("now", System.currentTimeMillis() + "");
        // while (true) {
        try {
            // receive.send(d, "45.125.46.48", 11378);
            receive.send(d, "127.0.0.1", 6465);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
