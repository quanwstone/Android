package com.qw.audiocapture;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2019/11/14.
 */


public class VideoEncode {
    private static String TAG = VideoEncode.class.getName();

    private MediaCodecInfo m_info = null;//定义CodecInfo
    private String m_strcodec;//codec名称
    private String m_strType;//codec 类型
    private int m_codeccount;//codec个数
    private MediaCodec m_codec;//codec对象

    private int m_H;
    private int m_W;
    private int m_Fps;
    private int m_Mode;
    private int m_Gop;
    private int m_BitRate;
    private int m_Color;
    private int m_Profile;
    private int m_Level;

    private ByteBuffer[] m_InputBuffer;
    private ByteBuffer[] m_OutputBuffer;

    private long m_TimeDelay;
    private MediaCodec.BufferInfo m_pBufferInfo;

    private File m_pFile;
    private FileInputStream m_InputFile;

    private File m_pFileDest;
    private FileOutputStream m_OutputFile;

    private boolean m_bRun;
    private byte[] m_srcData;
    private byte[] m_outData;
    private int m_iFrameSize;
    private ReadThread m_pThread;

    public VideoEncode()
    {
        m_H = 240;
        m_W = 320;
        m_Fps = 15;
        m_Gop = 1;
        m_BitRate = 240;//Kbps
        m_Mode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR;
        m_Profile =MediaCodecInfo.CodecProfileLevel.AVCProfileMain;
        m_Level = MediaCodecInfo.CodecProfileLevel.AVCLevel1b;

        m_pBufferInfo = new MediaCodec.BufferInfo();
        m_TimeDelay = -1;//INFINITE
        m_bRun = false;
        m_iFrameSize = m_H *m_W *3/2;
        m_srcData = new byte[m_iFrameSize];//NV21
        m_outData = new byte[m_iFrameSize];

    }
    public boolean InitPragram()
    {
        MediaCodecInfo.CodecCapabilities cs = m_info.getCapabilitiesForType(m_strType);//获取对应编码器类型的属性信息

        for(int i=0;i<cs.colorFormats.length;i++)
        {
            if(cs.colorFormats[i] == cs.COLOR_FormatYUV420SemiPlanar)
            {
                m_Color = cs.COLOR_FormatYUV420SemiPlanar;
                Log.d(TAG,"ColorForamt=="+m_Color);
            }
        }

        return true;
    }
    public boolean GetSuportForamt(String strcodec)
    {
        m_codeccount = MediaCodecList.getCodecCount();//获取codec个数
        Log.d(TAG,"codecCount="+m_codeccount);

        for(int i=0;i<m_codeccount;i++)
        {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);//获取codecinfo
            Log.d(TAG,"CodecName=="+info.getName());

            if(info.isEncoder())//判断当前codec是否支持编码
            {
                String[] str = info.getSupportedTypes();//获取支持的类型
                for(int j =0;j<str.length;j++)
                {
                    Log.d(TAG,"Type=="+str[j]+" Name=="+info.getName());

                    if(str[j].contains(strcodec))
                    {
                        m_info = info;
                        m_strcodec = m_info.getName();
                        m_strType = str[j];

                        Log.d(TAG,"get codecinfo.");
                    }
                    if(info.getName().contains(strcodec))//判断是否与希望的相同
                    {
                        m_info = info;
                        Log.d(TAG,"get codecinfo.");
                    }
                }
            }
        }


        return true;
    }
    public boolean pushdata(byte[] data,int iSize,byte[] outData,int[] iLen,int iCount) {

//        if(iCount % 10 == 0)设置一次将会一直输出I帧
//        {
//            Bundle params = new Bundle();
//            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//            m_codec.setParameters(params);//app level 19  //强制I帧
//            Log.d(TAG,"setParameters");
//        }
        int iIndex = m_codec.dequeueInputBuffer(m_TimeDelay);//获取一个可用的输入缓冲区
        if (iIndex >= 0) {
            ByteBuffer input = m_InputBuffer[iIndex];//获取输入队列中一个空闲的成员
            input.clear();
            input.put(data, 0, iSize);//填充数据

            long presentationtimeus =1000 * (iCount * 20);//用于设置时间戳 microseconds

            m_codec.queueInputBuffer(iIndex, 0, iSize, presentationtimeus,0);//填充一个ByteBuffer到输入队列,最后一个参数
        }else{
            Log.d(TAG,"dequeueInputBuffer Failed.");
            return false;
        }

        //同步获取编码后数据
        int iIndex2 = m_codec.dequeueOutputBuffer(m_pBufferInfo, m_TimeDelay);
        if (iIndex2 >= 0)
        {
            ByteBuffer output = m_OutputBuffer[iIndex2];
            output.position(m_pBufferInfo.offset);//设置缓冲区起始位置
            output.limit(m_pBufferInfo.offset + m_pBufferInfo.size);//设置缓冲区有效区间

            output.get(outData,0,m_pBufferInfo.size);//将有效数据写入数组
            iLen[0] = m_pBufferInfo.size;

            Log.d(TAG,"output offset="+m_pBufferInfo.offset+"Size"+m_pBufferInfo.size);
        }else{
            Log.d(TAG,"dequeueOutputBuffer Failed.");
            return false;
        }
        m_codec.releaseOutputBuffer(iIndex2,false);//回收缓存

        return true;
    }

    public boolean InitCodec()
    {
        try
        {
            //m_codec = MediaCodec.createByCodecName(m_strcodec);//创建MediaCodec,最小支持版本为16可通过build.gradle进行修改,当前方法不支持关键帧间隔设置，且不生成B帧
            m_codec = MediaCodec.createEncoderByType(m_strType);//创建MediaCodec.当前方法支持关键帧间隔设置.而且会生成B帧
            Log.d(TAG,"codec:"+m_strcodec+" Type:"+m_strType);
        }catch (IOException e)
        {
            e.printStackTrace();
        }

        MediaFormat vformat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,m_W,m_H);//获取MediaForamt对象

        vformat.setInteger(MediaFormat.KEY_FRAME_RATE,m_Fps);
        vformat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,m_Gop);
        vformat.setInteger(MediaFormat.KEY_WIDTH,m_W);
        vformat.setInteger(MediaFormat.KEY_HEIGHT,m_H);
        vformat.setInteger(MediaFormat.KEY_BITRATE_MODE,m_Mode);
        vformat.setInteger(MediaFormat.KEY_BIT_RATE,m_BitRate * 1000);//单位为bps
        vformat.setInteger(MediaFormat.KEY_COLOR_FORMAT,m_Color);
        //vformat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,0);
        vformat.setInteger(MediaFormat.KEY_PROFILE,m_Profile);
        vformat.setInteger(MediaFormat.KEY_LEVEL,m_Level);

        m_codec.configure(vformat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);//配置编码信息

        m_codec.start();//成功配置完成后调用开始

        m_InputBuffer = m_codec.getInputBuffers();//获取输入缓冲区 需要start之后调用.
        m_OutputBuffer = m_codec.getOutputBuffers();//获取输出缓冲区

        return true;
    }
    public boolean Start()
    {
        m_pThread = new ReadThread();
        m_pThread.start();
        m_bRun = true;

        return true;
    }

    public boolean Close()
    {
        m_bRun = false;

        try{
            m_pThread.join();
        }catch(InterruptedException e)
        {
            e.printStackTrace();
        }

        try{

            m_OutputFile.close();
            m_InputFile.close();

        }catch(IOException e)
        {
            e.printStackTrace();
        }


        return true;
    }

    public boolean openfile()
    {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+"Video.yuv";
        String path_dest =  Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+"video.h264";

        Log.d(TAG,path);

        try{
            m_pFile = new File(path);
            m_InputFile = new FileInputStream(m_pFile);

            m_pFileDest = new File(path_dest);
            if(!m_pFileDest.exists())
            {
                m_pFileDest.createNewFile();
            }
            m_OutputFile = new FileOutputStream(m_pFileDest);

        }catch(IOException e)
        {
            e.printStackTrace();
        }

        return true;
    }

    public class ReadThread extends  Thread
    {
        @Override
        public void run()
        {
            int[] iSize = new int[1];
            openfile();
            int iCount = 0;

            while(m_bRun)
            {
                try{
                    int ir = m_InputFile.read(m_srcData,0,m_iFrameSize);
                    if(ir> 0)
                    {
                        iCount++;

                        boolean br = pushdata(m_srcData,ir,m_outData,iSize,iCount);
                        if(br)
                        {
                            m_OutputFile.write(m_outData,0,iSize[0]);
                        }
                    }else
                    {
                        break;
                    }
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
