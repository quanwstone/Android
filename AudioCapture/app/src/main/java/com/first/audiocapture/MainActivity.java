package com.first.audiocapture;
/*
使用视频采集时需要添加权限请求的代码。
* */
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.first.audiocapture.VideoCapture;
import com.first.audiocapture.VideoSurfaceView;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static String Tag = MainActivity.class.getName();
    private VideoCapture m_pCapture = null;
    private Button m_Button;
    private VideoSurfaceView sufview = null;

    //权限请求的回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                     int[] grantResults)
    {
        Log.d(Tag,"requestCode"+requestCode);
        Log.d(Tag,"permissions"+permissions);
        Log.d(Tag,"grant"+grantResults);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SDK22以上
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(Tag,"Granted");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }

        m_Button = (Button)findViewById(R.id.onVideoCapture);
        sufview = (VideoSurfaceView)findViewById(R.id.Video_View);

        //
        m_Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //
                    Thread openThread = new Thread(){
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            m_pCapture = new VideoCapture();
                            m_pCapture.InitDevice();

                            SurfaceHolder holder = sufview.getSurfaceHolder();

                            m_pCapture.StartCapture(null,holder);
                        }
                    };
                    openThread.start();
                }
            }
        );
    }
}
