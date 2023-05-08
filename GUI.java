import javax.imageio.ImageIO;
import javax.swing.*;

import liming.texthandle.IO;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GUI extends JFrame implements ActionListener {
    private JLabel imageLabel, fpsLabel;
    private JButton startButton, stopButton;
    private ImageIcon imageIcon;
    private BufferedImage bufferImage;
    IO.IOThread thread;

    public GUI() {
        setTitle("图片展示");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 初始化图片展示区
        imageIcon = new ImageIcon("path/to/image.jpg");
        imageLabel = new JLabel(imageIcon);
        add(imageLabel, BorderLayout.CENTER);

        // 初始化帧数显示区
        fpsLabel = new JLabel("0 fps");
        fpsLabel.setForeground(Color.RED);
        add(fpsLabel, BorderLayout.NORTH);

        // 初始化开始按钮
        startButton = new JButton("开始");
        startButton.addActionListener(this);
        add(startButton, BorderLayout.WEST);

        // 初始化结束按钮
        stopButton = new JButton("结束");
        stopButton.addActionListener(this);
        add(stopButton, BorderLayout.EAST);

        IO.IOData iod = new IO.IOData() {
            private long lastTime = System.currentTimeMillis();
            private int frameCount = 0;

            @Override
            public void getByte(byte[] data) {
                // System.out.println(data.length);
                new Thread(() -> {
                    try {
                        // 将二进制数据流转为图片
                        ByteArrayInputStream bis = new ByteArrayInputStream(data);
                        BufferedImage image = ImageIO.read(bis);
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

                        // 将缓冲区的内容绘制到GUI界面中
                        Image scaledImage = bufferImage.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(),
                                Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(scaledImage);
                        imageLabel.setIcon(icon);
                        imageLabel.repaint();

                        long currentTime = System.currentTimeMillis();
                        frameCount++;
                        if (currentTime - lastTime >= 1000) {
                            int fps = (int) (frameCount * 1000 / (currentTime - lastTime));
                            fpsLabel.setText(fps + " fps");
                            lastTime = currentTime;
                            frameCount = 0;
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }).start();
            }
        };
        System.out.println(iod.setTime(1 * 1000, 15));
        thread = IO.getScreen(iod, 0.7f, "jpg");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            // TODO: 点击开始按钮后的逻辑
            thread.start();
        } else if (e.getSource() == stopButton) {
            // TODO: 点击结束按钮后的逻辑
            thread.stop();
        }
    }

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.setVisible(true);
    }
}
