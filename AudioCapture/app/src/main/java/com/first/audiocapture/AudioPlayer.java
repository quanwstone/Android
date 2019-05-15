package com.first.audiocapture;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.util.Log;
import android.media.AudioTrack;

/**
 * Created by Administrator on 2019/5/14.
 * 音频的播放流程使用AudioTrack流程如下：
 * 1）初始化音频参数然后创建AudioTrack对象
 * 2）启动播放
 * 3）在播放线程里对AudioTrack写入播放数据
 */

public class AudioPlayer {
    private static String Tag = AudioCapture.class.getName();
    private AudioTrack m_pAudioTrack = null;

    private int m_iAudioType = AudioManager.STREAM_MUSIC;   //声音类型
    private int m_iSampleRate = 44100;                          //声音采样率
    private int m_iChannel = AudioFormat.CHANNEL_OUT_MONO;     //声音声道数
    private int m_iFormat = AudioFormat.ENCODING_PCM_16BIT;     //声音格式
    private byte[] m_chDataBuff = null;                         //声音存放缓存
    private int m_iDataSize = 0;                                 //声音缓存长度
    private int m_iMode = AudioTrack.MODE_STREAM;               //模式和static的区别在于stream更通用和内存相对较大.

    private boolean m_iThreadFlag = false;                      //线程启动标志
    private AudioPlayThread m_pThread = null;                       //播放线程
    //初始化设备
    public boolean InitDevice()
    {
        Log.d(Tag,"InitDevice");

        m_iDataSize = AudioTrack.getMinBufferSize(m_iSampleRate,m_iChannel,m_iFormat);
        m_chDataBuff = new byte[m_iDataSize];

        m_pAudioTrack = new AudioTrack(m_iAudioType,m_iSampleRate,m_iChannel,m_iFormat,m_iDataSize,m_iMode);
        return true;
    }
    //开始播放
    public boolean StartPlay()
    {
        Log.d(Tag,"StartPlay Begin.");
        assert(m_pAudioTrack != null);

        m_iThreadFlag = true;
        m_pThread = new AudioPlayThread();
        m_pThread.start();

        m_pAudioTrack.play();

        return true;
    }
    //停止播放
    public boolean Close()
    {
        Log.d(Tag,"Close");

        m_iThreadFlag = false;
        m_pAudioTrack.release();

        return true;
    }
    public class AudioPlayThread extends Thread
    {
        @Override
        public void run()
        {
            while(m_iThreadFlag)
            {
                m_pAudioTrack.write(m_chDataBuff,0,m_iDataSize);//写入数据.
            }
        }
    }
}
