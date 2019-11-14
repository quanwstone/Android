package com.first.opengles;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2019/11/12.
 */

public class ShaderUtil {
    private static String Tag = ShaderUtil.class.getName();

    //读取顶点着色器和片源着色器转换成String.
    public static String readRawTxt(Context context, int rawId){
        InputStream inputStream = context.getResources().openRawResource(rawId);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuffer sb = new StringBuffer();

        String str_line;

        try{
            while((str_line = reader.readLine()) != null)
            {
                sb.append(str_line).append("\n");
            }
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        return sb.toString();
    }

    //创建顶点着色器和片源着色器
    public static int loadShader(int shaderType,String str) {
        int shader = GLES20.glCreateShader(shaderType);
        if( shader != 0)
        {
            GLES20.glShaderSource(shader,str);//着色器源码加载入着色器对象
            GLES20.glCompileShader(shader);//编译已存储在shader指定的着色器对象中的源代码字符串

            int[] compile = new int[1];
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compile,0);//获取着色器编译状态
            if(compile[0] != GLES20.GL_TRUE)
            {
                GLES20.glDeleteShader(shader);
                Log.d(Tag,"glComileShader failed.");
                shader = 0;
            }
        }
        return shader;
    }

    //创建着色器程序
    public static int createProgram(String Strvertex,String Strfragment){

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,Strvertex);//创建顶点着色器
        if(vertexShader == 0)
        {
            return 0;
        }

        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,Strfragment);//创建片源着色器
        if(fragmentShader == 0)
        {
            return 0;
        }

        int iprogram = GLES20.glCreateProgram();//创建着色器程序
        if(iprogram != 0)
        {
            GLES20.glAttachShader(iprogram,vertexShader);//将着色器添加到着色器程序上
            GLES20.glAttachShader(iprogram,fragmentShader);//将着色器添加到着色器程序上
            GLES20.glLinkProgram(iprogram);//将着色器程序链接

            int[] iStatus = new int[1];
            GLES20.glGetProgramiv(iprogram,GLES20.GL_LINK_STATUS,iStatus,0);//获取着色器程序链接状态
            if(iStatus[0] != GLES20.GL_TRUE)
            {
                GLES20.glDeleteProgram(iprogram);
                return 0;
            }
        }
        return iprogram;
    }
}
