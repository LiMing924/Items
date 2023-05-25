package liming.texthandle;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class IO {
    /**
     * 时间间隔类
     * 设置帧视频的帧率，及对获取到的视频的处理
     */
    public static abstract class IOData {
        /**
         * 如何处理得到帧图片
         * 
         * @param time  图片的时间戳
         * @param image 图片
         */
        public abstract void getImage(long time, BufferedImage image);

        // 默认的帧间隔时间
        private long time = 1 * 1000 / 30;

        /**
         * 设置视频帧率 在AllTime内获取frameRate张图片
         * 
         * @param AllTime   总时间
         * @param frameRate 总张数
         * @return time
         */
        public long setTime(long AllTime, float frameRate) {
            return time = (long) Math.ceil(AllTime / frameRate);
        }
    }

    /**
     * 获取IOScreen对象 传入IOData，图片压缩比，图片的格式，缓存大小
     *
     * @param io      IOData
     * @param quality 图片压缩比 原图为1.0
     * @param code    图片的格式 如png，jpg
     * @param factor  缓冲大小 基础缓冲1秒的数据，当请求超过1秒的数据会默认丢弃
     * @return
     */
    public static IOScreen getScreen(IOData io, float quality, String code, float factor) {
        return IOScreen.getScreen(io, quality, code, factor);
    }

    /**
     * 采用异步线程获取屏幕io流
     */
    public static class IOScreen {
        private ExecutorService executorService;
        private Thread getThread;
        private volatile boolean running = true;
        private State state;
        private IOData io;
        private String code;// 设置图片格式
        private float quality;// 设置图片的压缩比
        private final long TimeOut;

        private IOScreen(IOData io, float quality, String code, float factor) {
            state = new State(io, factor);
            this.io = io;
            this.code = code;
            this.quality = quality;
            // executorService = Executors.newFixedThreadPool((int) state.max * 2);
            TimeOut = 400;
        }

        public static IOScreen getScreen(IOData io, float quality, String code, float factor) {
            return new IOScreen(io, quality, code, factor);
        }

        public byte[] compressImage(BufferedImage image) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(code);
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.close();
            writer.dispose();
            return baos.toByteArray();
        }

        public void start() {
            running = true;
            if (getThread != null && !getThread.isAlive()) {
                getThread.interrupt();
            }

            if (addThread != null && !addThread.isAlive()) {
                addThread.interrupt();
            }

            if (datas != null)
                datas.clear();
            else
                datas = new ArrayList<>();
            executorService = Executors.newFixedThreadPool((int) state.max * 2);
            addThread = new Thread(new AddRunnable());
            getThread = new Thread(new GetRunnable());
            getThread.start();
            addThread.start();

            System.out.println("已启动1");
        }

        public void stop() {
            running = false;
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executorService.shutdownNow();
        }

        private List<Data> datas;

        private Thread addThread;

        private void add(Data data) {
            synchronized (datas) {
                // System.out.println("添加截图数据");
                datas.add(data);
                datas.notify();
            }
        }

        private class Data {
            long time;
            BufferedImage image;

            private Data(long time, BufferedImage image) {
                this.time = time;
                this.image = image;
            }
        }

        // 向上层提交图片数据的任务
        private class AddRunnable implements Runnable {
            public void run() {
                synchronized (datas) {
                    while (running) {
                        // System.out.println("轮回处理截图数据");
                        while (!datas.isEmpty()) {
                            Data data = datas.remove(0);
                            if (data.time < System.currentTimeMillis() - TimeOut * 2)
                                continue;
                            new Thread(() -> {
                                // System.out.println("截图数据回传");
                                io.getImage(data.time, data.image);
                            }).start();
                        }
                        // System.out.println("截图数据暂时处理完毕,进入等待");
                        try {
                            datas.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // System.out.println("等待结束");
                    }
                }
            }
        }

        private Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        // 循环获取屏幕图片的任务
        private class GetRunnable implements Runnable {
            public void run() {
                long StartTime = System.currentTimeMillis();
                while (running) {
                    long time = System.currentTimeMillis();
                    executorService.execute(() -> {
                        Thread thread = new Thread(new MRunnable(time));
                        thread.start();
                        try {
                            thread.join(TimeOut);
                            thread.join();
                        } catch (InterruptedException e) {
                            thread.interrupt();
                        }
                        if (thread.isAlive())
                            thread.interrupt();
                    });
                    try {
                        Thread.sleep(io.time - (System.currentTimeMillis() - StartTime) % io.time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private class State {
            private long num;
            private long max;

            private State(IOData io, float factor) {
                max = (long) Math.ceil((1 * 1000 / io.time) * factor);
                num = 0;
            }

            //
            private synchronized long remove() {
                return --num;
            }

            private synchronized boolean isAdd() {
                if (num < max) {
                    num++;
                    return true;
                } else
                    return false;
            }
        }

        // 获取单张图片的任务
        private class MRunnable implements Runnable {
            long time;

            public MRunnable(long time) {
                this.time = time;
            }

            @Override
            public void run() {

                {
                    long time = System.currentTimeMillis();
                    if (this.time + TimeOut < time)
                        return;
                }
                boolean run = false;

                try {
                    if (!state.isAdd())
                        return;
                    run = true;
                    long time = System.currentTimeMillis();
                    Robot robot = new Robot();
                    Rectangle screenRect = new Rectangle(dimension);
                    BufferedImage screenCapture = robot.createScreenCapture(screenRect);
                    add(new Data(time, screenCapture));
                } catch (Exception e) {
                } finally {
                    if (run) {
                        state.remove();
                    }
                }
            }
        }
    }
}