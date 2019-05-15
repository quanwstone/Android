package com.first.audiocapture;
/**
 * Created by Administrator on 2019/5/14.
 * 视频采集利用camera流程如下：
 * 1）打开采集设备用于获取一个Camera对象
 * 2）设置采集参数
 * 3）设置回调对象，和实现回调方法.
 * 4）启动采集
 */
//

//
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.hardware.Camera;

import java.io.IOException;
import java.util.List;

import android.util.Size;
import android.view.SurfaceHolder;

public class VideoCapture implements  Camera.PreviewCallback{
    private static String Tag = VideoCapture.class.getName();

    private Camera m_pCamera = null;            //视频采集设备对象.
    private int m_iCaptureFormat= ImageFormat.NV21; //采集格式
    private int m_iW = 320;                         //采集的宽度
    private int m_iH = 240;                         //采集的高度
    private int m_iFps = 15;                        //采集的帧率
    private byte[][] m_chDataBuff = null;            //用于存放采集的数据
    private int m_iDataLen = 0;                     //每帧画面的缓存长度.
    //初始化设备
    public boolean InitDevice()
    {
        Log.d(Tag,"InitDevice");

        //m_pCamera = Camera.open();              //打开设备默认后置摄像头(失败的原因是请求权限设置)
        int iCamera = Camera.getNumberOfCameras();
        for(int i=0;i<iCamera;i++)
        {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i,info);
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                m_pCamera = Camera.open(i);
            }
        }
        if(m_pCamera != null)
        {
            //设置参数信息
            Camera.Parameters parm = m_pCamera.getParameters();

            //通过获取的参数对象进行参数设置
            parm.setPreviewFormat(m_iCaptureFormat);

            //查询支持的帧率
            List<int[]> fpsR = parm.getSupportedPreviewFpsRange();
            for(int i=0;i<fpsR.size();i++)
            {
                int[] R = fpsR.get(i);
                Log.d(Tag,"FPS="+R[0]);
            }
            //获取支持的预览
            List<Camera.Size> Siz = parm.getSupportedPreviewSizes();
            for(int i=0;i<Siz.size();i++)
            {
                Camera.Size sz = Siz.get(i);
                Log.d(Tag,"W:"+sz.width + "H:"+sz.height);
            }

            parm.setPreviewSize(m_iW,m_iH);
            parm.setPreviewFrameRate(m_iFps);
            parm.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            m_iDataLen = m_iW *m_iH *ImageFormat.getBitsPerPixel(m_iCaptureFormat) / 8;
            m_chDataBuff = new byte[5][m_iDataLen];

            //画面选中90度，只是针对预览，真实数据不变.
            m_pCamera.setDisplayOrientation(90);
            m_pCamera.setParameters(parm);
        }
        return true;
    }

    //开始采集
    public boolean StartCapture(SurfaceTexture suf,SurfaceHolder hol) {
        Log.d(Tag, "StartCapture");
        assert (m_pCamera != null);

        if (suf != null)
        {
            try
            {
                m_pCamera.setPreviewTexture(suf);
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        //
       if(hol != null)
       {
           try
           {
               m_pCamera.setPreviewDisplay(hol);
           }catch(IOException e)
           {
               e.printStackTrace();
           }
       }
        //
        for(int i=0;i<5;i++)
        {
            m_pCamera.addCallbackBuffer(m_chDataBuff[i]);
        }

        m_pCamera.setPreviewCallbackWithBuffer(this);
        m_pCamera.startPreview();

        return true;
    }

    @Override//视频采集的回调函数
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        //读取数据
        //Log.d(Tag,"onPreviewFrame data Len="+data.length);
        //加载到采集队列
        m_pCamera.addCallbackBuffer(data);
    }
}
