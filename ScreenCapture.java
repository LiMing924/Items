import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import liming.texthandle.IO;
import liming.texthandle.IO.IOData;

public class ScreenCapture {

    private MScreen mScreen;

    private int Fps = 0;
    private int length = 0;

    private float quality = 0.8f;// 设置图片的压缩比
    private String code = "jpg";// 设置图片格式
    private float factor = 0.2f;// 获取时为避免线程过多性能衰减，设置在同时运行的线程数，占帧率的百分比
    private int frame = 30;// 帧率
    private float buffer = 0.1f;// 与一秒比较，缓冲的大小

    private IO.IOScreen ioscreen;

    public ScreenCapture() {
    }

    public ScreenCapture(float quality, String code, float factor, int frame, float buffer) {
        this.quality = quality;
        this.code = code;
        this.factor = factor;
        this.frame = frame;
        this.buffer = buffer;
    }

    public int getFps() {
        return Fps;
    }

    public int getGap() {
        if (mScreen == null)
            return -1;
        return mScreen.getGap();
    }

    public byte[] getImgByte(BufferedImage image) throws IOException {
        return ioscreen.compressImage(image);
    }

    public void start() {
        mScreen = new MScreen();
        // 创建一个IOData对象，用于接收截屏数据
        IOData ioData = new IOData() {
            private long lastTime = System.currentTimeMillis();
            private int frameCount = 0;

            // 更新每秒帧数
            private void updateFPS() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime >= 1000) {
                    int fps = (int) (frameCount * 1000 / (currentTime - lastTime));
                    Fps = fps;
                    lastTime = currentTime;
                    frameCount = 0;
                }
            }

            @Override
            public void getImage(long time, BufferedImage image) {
                // 将截屏数据添加到MScreen对象中
                System.out.print("\r" + Fps + " fps  " + length);
                mScreen.add(time, image);
                frameCount++;
                updateFPS();
            }
        };
        // 获取屏幕截图
        ioData.setTime(1000, frame);
        ioscreen = IO.getScreen(ioData, quality, code, factor);
        ioscreen.start();
    }

    public void stop() {
        if (ioscreen != null) {
            ioscreen.stop();
        }
    }

    // 传入需获取的时间结点，返回
    public List<BufferedImage> get(long now_time, long get_time) {
        try {
            // 获取截屏数据并将其转换为视频
            return mScreen.get(now_time, get_time);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<byte[]> getByte(long now_time, long get_time) {
        try {
            // 获取截屏数据并将其转换为视频
            return mScreen.getByte(now_time, get_time);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class MScreen {

        private int size;// 自动计算的最大缓冲长度
        private List<Time_Data> datas;
        // private final ReentrantLock lock = new ReentrantLock();
        private long start_time;// 开始的时间
        private int gap;// 由帧率计算的帧间时长

        public MScreen() {
            start_time = System.currentTimeMillis();
            size = (int) Math.ceil(frame * (1 + buffer));
            datas = new ArrayList<>();
            gap = (int) Math.ceil(1000 / frame);
        }

        public int getGap() {
            return gap;
        }

        public BufferedImage add(long time, BufferedImage image) {
            length = image.getHeight() * image.getWidth();
            Time_Data time_Data = new Time_Data(time, image);
            // lock.lock();
            synchronized (datas) {
                if (datas.size() == 0) {
                    datas.add(time_Data);
                } else {
                    int i = 0;
                    for (; i < datas.size(); i++) {
                        if (datas.get(i).getStartSerial() > time_Data.getStartSerial()) {
                            break;
                        }
                    }
                    datas.add(i, time_Data);
                }
                if (datas.size() > size) {
                    datas.remove(0);
                }
            }
            return image;
        }

        public List<BufferedImage> get(long now_time, long get_time) throws IOException {
            List<Time_Data> copy;
            // lock.lock();
            synchronized (datas) {
                copy = new ArrayList<>(datas);
            }
            // lock.unlock();
            if (copy.isEmpty()) {
                System.out.print("\r缓冲为空");
                return null;
            }
            List<BufferedImage> datas = new ArrayList<>();
            for (Time_Data data : copy) {
                if (data.getStartSerial() > (now_time - start_time) / gap + 1)
                    break;
                if (data.getStartSerial() < (now_time - get_time - start_time) / gap - 1)
                    continue;
                datas.add(data.getImage());
            }
            return datas;
        }

        public List<byte[]> getByte(long now_time, long get_time) throws IOException {
            List<Time_Data> copy;
            // lock.lock();
            synchronized (datas) {
                copy = new ArrayList<>(datas);
            }
            // lock.unlock();
            if (copy.isEmpty()) {
                System.out.print("\r缓冲为空");
                return null;
            }
            List<byte[]> datas = new ArrayList<>();
            for (Time_Data data : copy) {
                if (data.getStartSerial() > (now_time - start_time) / gap + 1)
                    break;
                if (data.getStartSerial() < (now_time - get_time - start_time) / gap - 1)
                    continue;
                datas.add(data.getByte());
            }
            return datas;
        }

        private class Time_Data {
            private long time;// 创建时间
            private BufferedImage image;// 图像信息
            private byte[] bytes;
            private long num;// 图像序号
            private Object key = new Object();

            public Time_Data(long time, BufferedImage image) {
                this.image = image;
                this.time = time;
                num = 0;
                new Thread(() -> {
                    synchronized (key) {
                        try {
                            bytes = getImgByte(image);
                        } catch (IOException e) {
                            System.out.println(this);
                        }
                    }
                }).start();
            }

            public BufferedImage getImage() {
                return image;
            }

            public byte[] getByte() {
                synchronized (key) {
                    return bytes;
                }
            }

            // // 判断截屏数据是否过期
            // public boolean equals() {
            // return time + 1000 + 1000 * buffer < System.currentTimeMillis();
            // }

            // 获取该截屏数据的序列号
            public long getStartSerial() {
                if (num == 0) {
                    long nowTime = time - start_time;
                    num = nowTime / gap + 1;
                }
                return num;
            }

            // 获取该截屏数据在MScreen对象中的序列号
            public int getSerial() {
                long nowTime = System.currentTimeMillis() - time;
                return size - (int) nowTime / gap - 1;
            }

            @Override
            public String toString() {
                return "Time_Data [time=" + time + ", data="
                        + (image == null ? null : image.getHeight() * image.getWidth()) + ", num=" + num
                        + ", serial=" + getSerial() + "]";
            }

        }
    }
}
