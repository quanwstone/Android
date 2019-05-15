package com.first.audiocapture;

/**
 * Created by Administrator on 2019/5/14.
 * 利用AudioRecord实现音频采集流程如下：
 * 1）设置音频采样参数,创建AudioRecord对象.
 * 2）启动采集
 * 3）在采集线程里循环读取数据.
 */
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioCapture {

    private static  String Tag = AudioCapture.class.getName();
    private AudioRecord m_pAudioRecord = null;

    private int m_iAudioSource = MediaRecorder.AudioSource.MIC;                 //采集来源为麦克风
    private int m_iSampleRate = 44100;                                          //采样率设置为44.1khz
    private int m_iAudioFormat = AudioFormat.ENCODING_PCM_16BIT;             //采样格式
    private int m_iBufferSize = 0;                                              //缓存区大小
    private int m_iChannel = AudioFormat.CHANNEL_IN_MONO;                     //通道个数

    private boolean            m_iThreadFlag = false;                                    //采集线程标志
    private AudioCaptureThread  m_pCaptureThread = null;                        //采集线程
    private byte[]             m_pDataBuff = null;                                     //数据存放数组
    //初始化设备
    public boolean InitDevice()
    {
        Log.d(Tag,"InitDevice Begin.");

        m_iBufferSize = AudioRecord.getMinBufferSize(m_iSampleRate,m_iChannel,m_iAudioFormat);

        m_pDataBuff = new byte[m_iBufferSize];
        Log.d(Tag,"getMinBufferSize Size="+m_iBufferSize);

        m_pAudioRecord = new AudioRecord(m_iAudioSource,m_iSampleRate,m_iChannel,m_iAudioFormat,m_iBufferSize);
        if(null == m_pAudioRecord)
        {
            Log.e(Tag,"new AudioRecord Failed.");
            return false;
        }

        Log.d(Tag,"InitDevice Success.");
        return true;
    }

    //启动采集
    public boolean StartCapture()
    {
        Log.d(Tag,"StartCapture Begin.");
        assert(m_pAudioRecord != null);

        m_pCaptureThread = new AudioCaptureThread();
        m_pCaptureThread.start();
        m_pAudioRecord.startRecording();

        return true;
    }
    //关闭采集
    public boolean Close()
    {
        Log.d(Tag,"Close");

        m_iThreadFlag = false;
        m_pAudioRecord.release();

        return true;
    }
    public class AudioCaptureThread extends Thread
    {
        @Override
        public void run()
        {
            Log.d(Tag,"AudioCaptureThread run Begin.");

            while(m_iThreadFlag)
            {
                int iR = m_pAudioRecord.read(m_pDataBuff,0,m_iBufferSize);
                if(iR != 0)
                {
                    Log.d(Tag,"m_pAudioRecord.read Size="+iR);
                }
            }

            Log.d(Tag,"AudioCaptureThread run End.");
        }
    }
}
