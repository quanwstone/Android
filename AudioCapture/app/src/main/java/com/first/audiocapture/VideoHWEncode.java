package com.first.audiocapture;

/**
 * Created by Administrator on 2019/5/15.
 *硬件编码利用MediaCodec流程如下：
 * 1）根据名称创建对应的编码器
 * 2）设置编码器参数
 * 3）通过输入输出队列进行编码，
 * 4）编码后数据需要进行手动组合，每个I帧前需要填充sps和pps，
 */
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.media.MediaCodecInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoHWEncode {
    private static String TAG = VideoHWEncode.class.getName();
    // video device.
    private MediaCodec vencoder;
    private MediaCodecInfo vmci;
    private MediaCodec.BufferInfo vebi;
    private byte[] vbuffer;
    // video camera settings.
    private static final String VCODEC = "video/avc";       //编码器类型
    private int vcolor;                    //编码格式
    private int vbitrate_kbps = 300;     //码率大小kbps
    private final static int VFPS = 15; //FPS长度
    private final static int VGOP = 5;  //GOP长度
    private int m_iW = 320;         //原始数据高度
    private int m_iH = 240;         //原始数据宽度
    private int m_iMode =MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR;   //码率格式

    private int m_iTimeOut = -1;// 表示一直等待类似于INFINIT
    ByteBuffer[] m_inputBuffer = null;      //用于编码输入队列
    ByteBuffer[] m_outputBuffer = null;     //用于编码输出队列

    /* 首先需要初始化MediaCodec的配置 */
    private void initMediaCodec() {

        vcolor = chooseVideoEncoder();
        try {
            vencoder = MediaCodec.createByCodecName(vmci.getName());
        } catch (IOException e) {
            Log.e(TAG, "create vencoder failed.");
            e.printStackTrace();
            return;
        }
        vebi = new MediaCodec.BufferInfo();
        // setup the vencoder.
        // @see https://developer.android.com/reference/android/media/MediaCodec.html
        MediaFormat vformat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, m_iW, m_iH);

        vformat.setInteger(MediaFormat.KEY_COLOR_FORMAT, vcolor);
        vformat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        vformat.setInteger(MediaFormat.KEY_BIT_RATE, 1000 * vbitrate_kbps);
        vformat.setInteger(MediaFormat.KEY_FRAME_RATE, VFPS);
        vformat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VGOP);
        vformat.setInteger(MediaFormat.KEY_BITRATE_MODE,m_iMode);
        vencoder.configure(vformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        vencoder.start();

        m_inputBuffer = vencoder.getInputBuffers();
        m_outputBuffer = vencoder.getOutputBuffers();

    }

    public boolean VideoEncode(byte[] str,int iSrc,byte[] out,int iOut)
    {
        int index = vencoder.dequeueInputBuffer(m_iTimeOut);
        if(index >=0)
        {
            ByteBuffer inputBuffer = m_inputBuffer[index];
            inputBuffer.clear();
            inputBuffer.put(str,0,iSrc);

            long presentationTimeUs = System.currentTimeMillis();
            vencoder.queueInputBuffer(index,0,iSrc,presentationTimeUs,0);
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        int encoderR = vencoder.dequeueOutputBuffer(info,m_iTimeOut);
        if(encoderR >=0)
        {
            ByteBuffer encodeData = m_outputBuffer[encoderR];
            encodeData.position(info.offset);
            encodeData.limit(info.offset+info.size);

            encodeData.get(out,0,info.size);
            iOut = info.size;
        }
        return true;
    }
    public void Close()
    {
        vencoder.release();

    }
    // for the vbuffer for YV12(android YUV), @see below:
// https://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat(int)
// https://developer.android.com/reference/android/graphics/ImageFormat.html#YV12
    private int getYuvBuffer(int width, int height) {
        // stride = ALIGN(width, 16)
        int stride = (int) Math.ceil(width / 16.0) * 16;
        // y_size = stride * height
        int y_size = stride * height;
        // c_stride = ALIGN(stride/2, 16)
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        // c_size = c_stride * height/2
        int c_size = c_stride * height / 2;
        // size = y_size + c_size * 2
        return y_size + c_size * 2;
    }

    // choose the video encoder by name.
    private MediaCodecInfo chooseVideoEncoder(String name, MediaCodecInfo def) {
        int nbCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < nbCodecs; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (!mci.isEncoder()) {
                continue;
            }
            String[] types = mci.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(VCODEC)) {
                    //Log.i(TAG, String.format("vencoder %s types: %s", mci.getName(), types[j]));
                    if (name == null) {
                        return mci;
                    }

                    if (mci.getName().contains(name)) {
                        return mci;
                    }
                }
            }
        }
        return def;
    }

    // choose the right supported color format. @see below:
// https://developer.android.com/reference/android/media/MediaCodecInfo.html
// https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities.html
    private int chooseVideoEncoder() {
        // choose the encoder "video/avc":
        //      1. select one when type matched.
        //      2. perfer google avc.
        //      3. perfer qcom avc.
        vmci = chooseVideoEncoder(null, null);

        int matchedColorFormat = 0;

        MediaCodecInfo.CodecCapabilities cc = vmci.getCapabilitiesForType(VCODEC);
        for (int i = 0; i < cc.colorFormats.length; i++) {
            int cf = cc.colorFormats[i];
            Log.i(TAG, String.format("vencoder %s supports color fomart 0x%x(%d)", vmci.getName(), cf, cf));

            // choose YUV for h.264, prefer the bigger one.
            // corresponding to the color space transform in onPreviewFrame
            if ((cf >= cc.COLOR_FormatYUV420Planar && cf <= cc.COLOR_FormatYUV420SemiPlanar)) {
                if (cf > matchedColorFormat) {
                    matchedColorFormat = cf;
                }
            }
        }
        for (int i = 0; i < cc.profileLevels.length; i++) {
            MediaCodecInfo.CodecProfileLevel pl = cc.profileLevels[i];
            Log.i(TAG, String.format("vencoder %s support profile %d, level %d", vmci.getName(), pl.profile, pl.level));
        }
        Log.i(TAG, String.format("vencoder %s choose color format 0x%x(%d)", vmci.getName(), matchedColorFormat, matchedColorFormat));
        return matchedColorFormat;
    }
}


