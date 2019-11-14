package com.first.opengles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Administrator on 2019/11/13.
 */

public class GLSurface extends GLSurfaceView {

    private static String Tag = GLSurface.class.getName();

    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "Video.yuv";
    private File pFile = null;
    private FileInputStream pInput = null;
    private byte[] data = new byte[320*240*3/2];
    private byte[]y = new byte[320*240];
    private byte[]u = new byte[320*240/4];
    private byte[]v = new byte[320*240/4];

    private Render render;
    private boolean gfloag = false;

    public void setgloag(boolean b)
    {
        gfloag = b;
    }
    public GLSurface(Context context)
    {
        this(context,null);
    }

    public GLSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.d(Tag,"GLSurface Begin");

        setEGLContextClientVersion(2);//设置使用的OpengGl版本.

        render = new Render(context,this);

        setRenderer(render);//设置Renderer
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//需要手动调用requesetRender触发onDrawFrame方法

    }

    public void setYUVData(int w,int h,byte[] y,byte[] u, byte[] v)
    {
        render.pushdata(h,w,y,u,v);

        requestRender();
    }

    private void SplicNV21YUV(byte[] yuv)//yyyy yyyy uv uv uv uv
    {
        System.arraycopy(data,0,y,0,320*240);

        int j = 0;
        for(int i=0;i< 320 * 240 / 2;i+=2)
        {
            u[j] = data[320*240 + i];
            v[j] = data[320*240 + i+1];
            j++;
        }
    }
    private void OpenFile()
    {
        try{
            pFile = new File(path);
            pInput = new FileInputStream(pFile);
        }catch(FileNotFoundException e )
        {
            e.printStackTrace();
        }
    }
    public void Start()
    {
        Log.d(Tag,"Start Begin.");
//        OpenFile();
//
//        try {
//            int iR = pInput.read(data,0,320*240*3/2);
//            while(iR > 0)
//            {
//                Log.d("setYUVData","setYUVData.");
//                SplicNV21YUV(data);
//
//                setYUVData(320,240,y,u,v);
//
//                iR = pInput.read(data,0,320*240*3/2);
//
//                try {
//                    Thread.sleep(200);
//                }catch(InterruptedException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//            Log.d("While","Break While.");
//        }catch (IOException e)
//        {
//            e.printStackTrace();
//        }

        PushDataThread thrad = new PushDataThread();
        thrad.start();
    }
    public class PushDataThread extends Thread{
        //读取文件
        @Override
        public void run()
        {
            OpenFile();

            try{
                int iR = pInput.read(data,0,320*240*3/2);
                while(iR > 0)
                {
                    if(gfloag)
                    {
                        Log.d("PushDataThread","setYUVData");
                        SplicNV21YUV(data);

                        setYUVData(320,240,y,u,v);

                        iR = pInput.read(data,0,320*240*3/2);
                    }

                    try {
                        Thread.sleep(200);
                    }catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                Log.d("PushDataThread","Break");
            }catch(IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}
