import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class GUI extends JFrame implements ActionListener {
    private JLabel imageLabel, fpsLabel;
    private JButton startButton, stopButton;
    private ImageIcon imageIcon;
    private BufferedImage bufferImage;
    private ScreenCapture screenCapture;
    private List<byte[]> datas;

    private float quality = 0.5f;// 设置图片的压缩比
    private String code = "jpg";// 设置图片格式
    private float factor = 0.2f;// 获取时为避免线程过多性能衰减，设置在同时运行的线程数，占帧率的百分比
    private int frame = 24;// 帧率
    private float buffer = 0f;// 与一秒比较，缓冲的大小
    private long getTime = 0;// 延时设置，不可超过1000*(1+buffer)

    public GUI() {
        setTitle("图片展示");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

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
        screenCapture = new ScreenCapture(quality, code, factor, frame, buffer);

    }

    private Object key = new Object();
    private int gap;
    private boolean run;

    private void start() {
        run = true;
        gap = screenCapture.getGap();
        if (getTime <= gap)
            getTime = gap * 2 * ((int) Math.ceil(factor / 15));
        else
            getTime = (int) Math.ceil(getTime / gap);
        new Thread(() -> {
            while (run) {
                synchronized (key) {
                    datas = screenCapture.get(System.currentTimeMillis(), getTime);
                    fpsLabel.setText(screenCapture.getFps() + " fps");
                }
            }
        }).start();
    }

    private void Show() {
        List<byte[]> datas;
        synchronized (key) {
            if (this.datas == null)
                return;
            datas = new ArrayList<>(this.datas);
        }
        long start_time = System.currentTimeMillis();
        for (int i = 0; i < datas.size(); i++) {
            // 将二进制数据流转为图片
            long now_time = System.currentTimeMillis();
            int num = (int) (now_time - start_time) / gap;
            if (num == i)
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(datas.get(i));
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
                    Image scaledImage = bufferImage.getScaledInstance(imageLabel.getWidth(),
                            imageLabel.getHeight(),
                            Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaledImage);
                    imageLabel.setIcon(icon);
                    imageLabel.repaint();
                } catch (Exception e) {
                }
            else if (num > i)
                continue;
            try {
                Thread.sleep((System.currentTimeMillis() - start_time) % gap);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            // TODO: 点击开始按钮后的逻辑
            try {
                screenCapture.start();
                start();
                new Thread(() -> {
                    while (run) {
                        Thread thread1 = new Thread(() -> {
                            try {
                                Thread.sleep(getTime - gap);
                            } catch (InterruptedException e1) {
                            }
                        });
                        Thread thread2 = new Thread(() -> {
                            Show();
                        });
                        thread1.start();
                        thread2.start();
                        try {
                            thread1.join();
                            thread2.interrupt();
                        } catch (Exception e1) {
                        }

                    }
                }).start();
            } catch (Exception e1) {
                // TODO: handle exception
            }

        } else if (e.getSource() == stopButton) {
            // TODO: 点击结束按钮后的逻辑
            screenCapture.stop();
            run = false;
        }
    }

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.setVisible(true);
    }
}
