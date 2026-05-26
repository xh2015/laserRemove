package com.topsky.laserremove.manager;

import android.os.Handler;
import android.os.Looper;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;
import com.skydroid.fpvplayer.FrameInfoHelper;
import com.skydroid.fpvplayer.SkyVideoEncoder;
import com.skydroid.fpvplayer.VideoEncoderCallBack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author 咔一下
 * @date 2025/2/8 18:21
 * @email 1501020210@qq.com
 */
public class MP4RecordHelper {

    private int width = 1280;
    private int height = 720;
    private int frameRate = 15;
    private int bitrate = 1000000;
    private boolean useSoftEncoder = false;
    private String h264Path;
    private FileOutputStream h264FileOutputStream;

    private SkyVideoEncoder videoEncoder;

    private final String tempSuffix = ".h264";

    /**
     * 开始录像
     * @param path
     * @return null表示成功，!null表示失败
     */
    public synchronized Exception start(String path){
        File file = new File(path + tempSuffix);
        if (file.exists()){
            return new IOException("File already exists:" + path);
        }
        try {
            boolean retCreateNewFile = file.createNewFile();
            if (!retCreateNewFile){
                return new IOException("createNewFile fail:" + path);
            }
            this.h264Path = file.getAbsolutePath();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            SkyVideoEncoder videoEncoder = new SkyVideoEncoder();
            videoEncoder.setVideoEncoderCallBack(new VideoEncoderCallBack() {
                byte[] sps;
                byte[] pps;
                boolean isWaitSPSPPS = true;
                @Override
                public void onVideoEncodeData(int frameType, byte[] frame, long timestamp_us) {
                    switch (frameType){
                        case SkyVideoEncoder.H264_SPS:
                            sps = new byte[frame.length];
                            System.arraycopy(frame,0,sps,0,frame.length);
                            break;
                        case SkyVideoEncoder.H264_PPS:
                            pps = new byte[frame.length];
                            System.arraycopy(frame,0,pps,0,frame.length);
                            if (isWaitSPSPPS){
                                isWaitSPSPPS = false;
                            }
                            try {
                                fileOutputStream.write(sps);
                                fileOutputStream.write(pps);
                                fileOutputStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case SkyVideoEncoder.H264_IDR:
                            if (isWaitSPSPPS){
                                break;
                            }
                            byte[][] datas = FrameInfoHelper.splitH264NALU(frame);
                            try {
                                if (datas == null){
                                    fileOutputStream.write(frame);
                                }else{
                                    fileOutputStream.write(datas[2]);
                                }
                                fileOutputStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case SkyVideoEncoder.H264_FRAME:
                            if (isWaitSPSPPS){
                                break;
                            }
                            try {
                                fileOutputStream.write(frame);
                                fileOutputStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            });
            this.videoEncoder = videoEncoder;
            this.h264FileOutputStream = fileOutputStream;
            videoEncoder.start(width,height,frameRate,bitrate,useSoftEncoder);
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    public synchronized void stop(final MP4RecordCallBack callBack){
        final String h264Path = this.h264Path;
        final SkyVideoEncoder videoEncoder = this.videoEncoder;
        if (videoEncoder == null){
            if (callBack != null){
                callBack.onRecordComplete(new IllegalStateException("Not started"));
            }
            return;
        }
        final FileOutputStream h264FileOutputStream = this.h264FileOutputStream;
        videoEncoder.stop();
        this.videoEncoder = null;
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    if (h264FileOutputStream != null){
                        try {
                            h264FileOutputStream.flush();
                            h264FileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    FileDataSourceImpl dataSource = null;
                    try {
                        dataSource = new FileDataSourceImpl(h264Path);
                        H264TrackImpl h264Track = new H264TrackImpl(dataSource);
                        Movie movie = new Movie();
                        movie.addTrack(h264Track);
                        Container mp4file = new DefaultMp4Builder().build(movie);

                        String mp4Path = h264Path.substring(0, h264Path.length() - tempSuffix.length());
                        try (FileChannel fc = new FileOutputStream(new File(mp4Path)).getChannel()) {
                            mp4file.writeContainer(fc);
                        }
                    } finally {
                        if (dataSource != null) {
                            try {
                                dataSource.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    boolean delRet = new File(h264Path).delete();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callBack != null){
                                callBack.onRecordComplete(null);
                            }
                        }
                    });
                }catch (Exception e){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callBack != null){
                                callBack.onRecordComplete(e);
                            }
                        }
                    });
                }

            }
        }.start();
    }

    public void putYUV(ByteBuffer directBuffer, int width, int height, int pixel_format){
        SkyVideoEncoder videoEncoder = this.videoEncoder;
        if (videoEncoder != null){
            videoEncoder.putYUV(directBuffer,width,height,pixel_format);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setVideoSize(int width,int height) {
        this.width = width;
        this.height = height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public boolean isUseSoftEncoder() {
        return useSoftEncoder;
    }

    public void setUseSoftEncoder(boolean useSoftEncoder) {
        this.useSoftEncoder = useSoftEncoder;
    }

    public static interface MP4RecordCallBack{
        public void onRecordComplete(Exception e);
    }
}
