package com.first.opengles;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.first.audiocapture.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2019/11/12.
 */

public class Render implements GLSurfaceView.Renderer {

    private static String Tag = Render.class.getName();
    private Context context;
    private GLSurface gSurface = null;

    private int m_Program;//着色器程序
    private int[] m_texture;//

    private int avPosition;//顶点属性索引
    private int afPosition;//片源属性索引

    private FloatBuffer vertexBuffer;//存储顶点坐标数组
    private FloatBuffer textureBuffer;//存储纹理坐标数组

    private int m_H;//生成纹理图像的高
    private int m_W;//生成纹理图像的宽

    private ByteBuffer Y;//存放y数据
    private ByteBuffer U;//存放u数据
    private ByteBuffer V;//存放v数据

    private int sampler_y;//采样器
    private int sampler_u;
    private int sampler_v;
    private final float[] vertexData ={//顶点坐标

            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f

    };

    private final float[] textureData ={//纹理坐标
            0f,1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };

    public Render(Context ctx,GLSurface p)
    {
        context = ctx;
        gSurface = p;
    }
    private void initDataBuffer()
    {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length *4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }
    //初始化着色器程序和纹理
    private void initRenderYUV()
    {
        //读取顶点着色器和片源着色器
        String str_vertex = ShaderUtil.readRawTxt(context, R.raw.vertex_shader);
        String str_fragment = ShaderUtil.readRawTxt(context,R.raw.fragment_shader);

        m_Program = ShaderUtil.createProgram(str_vertex,str_fragment);//创建着色器程序
        avPosition = GLES20.glGetAttribLocation(m_Program,"av_Position");//获取顶点属性索引
        afPosition = GLES20.glGetAttribLocation(m_Program,"af_Position");//获取片源属性索引

        sampler_y = GLES20.glGetUniformLocation(m_Program,"sampler_y");
        sampler_u = GLES20.glGetUniformLocation(m_Program,"sampler_u");
        sampler_v = GLES20.glGetUniformLocation(m_Program,"sampler_v");

        m_texture = new int[3];
        GLES20.glGenTextures(3,m_texture,0);//创建纹理

        for(int i=0;i < 3;i++)
        {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,m_texture[i]);//绑定纹理
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);//设置S轴纹理环绕方式
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);//设置T轴纹理环绕方式
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);//设置纹理过滤方式
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);//设置纹理过滤方式
        }

        initDataBuffer();
    }

    //数据渲染
    private void readerYUV()
    {
        if(Y != null && U != null && V != null)
        {
            Log.d(Tag,"readerYUV W:"+m_W +"H:"+m_H);

            GLES20.glUseProgram(m_Program);//使用着色器程序

            GLES20.glEnableVertexAttribArray(avPosition);//启用顶点属性数组有效
            GLES20.glVertexAttribPointer(avPosition,2,GLES20.GL_FLOAT,false,8,vertexBuffer);

            GLES20.glEnableVertexAttribArray(afPosition);
            GLES20.glVertexAttribPointer(afPosition,2, GLES20.GL_FLOAT,false,8,textureBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//激活纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,m_texture[0]);//绑定纹理
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,m_W,m_H,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,Y);//转换成2D纹理图像

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,m_texture[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,m_W / 2,m_H/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,U);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,m_texture[2]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,m_W / 2,m_H/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,V);

            GLES20.glUniform1i(sampler_y,0);
            GLES20.glUniform1i(sampler_u,1);
            GLES20.glUniform1i(sampler_v,2);

            Y.clear();
            U.clear();
            V.clear();

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);//绘制
        }

    }

    public void pushdata(int iH,int iW,byte[] y,byte[] u,byte[] v)
    {
        m_H = iH;
        m_W = iW;
        Y = ByteBuffer.wrap(y);
        U = ByteBuffer.wrap(u);
        V = ByteBuffer.wrap(v);
    }
    @Override
    public void onSurfaceCreated(GL10 gl,EGLConfig config)
    {
        Log.d(Tag,"onSurfaceCreated Begin.");

        initRenderYUV();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(Tag,"onSurfaceChanged Begin.:W"+width +"H"+height);

        GLES20.glViewport(0,0,width,height);//设置绘制区域
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        Log.d(Tag,"onDrawFrame Begin.");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);

        readerYUV();

        gSurface.setgloag(true);
    }
}
