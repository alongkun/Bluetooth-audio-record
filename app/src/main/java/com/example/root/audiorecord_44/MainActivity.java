package com.example.root.audiorecord_44;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private Button button_start_record = null;
    private Button button_stop_record = null;
    private Button button_start_play = null;
    private Button button_stop_paly = null;

    private AudioManager audioManager = null;

    private RecordTask recordThread = null;
    private PlayTask audioPlayThread = null;
    private final String TAG = "MainActivity";
    private boolean mStartRecognize = false;
    private boolean mPlaying = false;
    private int mPlayOffset = 0;
    private int mPrimePlaySize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button_start_record = (Button) findViewById(R.id.button_start_record);
        button_stop_record = (Button) findViewById(R.id.button_stop_record);
        button_start_play = (Button) findViewById(R.id.button_start_play);
        button_stop_paly = (Button) findViewById(R.id.button_stop_play);

        button_start_record.setOnClickListener(button_listener);
        button_stop_record.setOnClickListener(button_listener);
        button_start_play.setOnClickListener(button_listener);
        button_stop_paly.setOnClickListener(button_listener);
        button_stop_paly.setEnabled(false);
        button_stop_record.setEnabled(false);
/*
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Log.d("Record Task", "not support BT");
        }
        Log.d("Record Task", "support BT");

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.startBluetoothSco();
        audioManager.setBluetoothScoOn(true);
*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (audioManager.isBluetoothScoOn()) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }

    }

    private View.OnClickListener button_listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId())
            {
                case R.id.button_start_record:
                    mStartRecognize = true;

                    recordThread = new RecordTask();
                    recordThread.execute();

                    break;
                case R.id.button_stop_record:
                    mStartRecognize = false;

                    break;
                case R.id.button_start_play:
                    mPlaying = true;

                    audioPlayThread = new PlayTask();
                    audioPlayThread.execute();

                    break;
                case R.id.button_stop_play:
                    mPlaying = false;

                    break;
                default:
                    break;
            }
        }
    };


    class RecordTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {

            String filePath = Environment.getExternalStorageDirectory() + "/record.pcm";
            Log.d(TAG, "开始录音");
            //16K采集率
            int sampleRate = 16000;
            //格式
            int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
            //16Bit
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
            int AUDIO_FORMAT_IN_BYTE = 2;
            int channelCnt = 1;
            int mBufSize = sampleRate * 20 / 1000 * channelCnt * AUDIO_FORMAT_IN_BYTE;
            File audioFile = new File(filePath);
            OutputStream os = null;
            try {
                os = new FileOutputStream(audioFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            //mWavWriter = new WavWriter(filePath, channelCnt, sampleRate, audioEncoding);
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfiguration, audioEncoding, bufferSize);
            byte[] data_pack = new byte[mBufSize];
            audioRecord.startRecording();
            while (mStartRecognize) {
                int len = audioRecord.read(data_pack, 0, mBufSize);
                if (len == AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
                    continue;
                }
                //mWavWriter.writeToFile(data_pack, len);
                try {
                    dos.write(data_pack, 0, len);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //mWavWriter.closeFile();
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioRecord.stop();
            audioRecord.release();

            return null;
        }

        /*
        // 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行
        protected void onProgressUpdate(Integer... progress) {
            stateView.setText(progress[0].toString());
        }
        */
        @Override
        protected void onPostExecute(Object o) {
            button_stop_record.setEnabled(false);
            button_start_record.setEnabled(true);
            button_start_play.setEnabled(true);
            button_stop_paly.setEnabled(false);
        }

        @Override
        protected void onPreExecute() {
            // stateView.setText("正在录制");
            button_start_record.setEnabled(false);
            button_start_play.setEnabled(false);
            button_stop_paly.setEnabled(false);
            button_stop_record.setEnabled(true);
        }
    }


    class PlayTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] object) {

            File audioFile = new File(Environment.getExternalStorageDirectory() + "/record.pcm");
            if (!audioFile.exists()) {
                Log.d(TAG, "录音文件不存在！");
                return null;
            }
            FileInputStream inStream;
            try {
                inStream = new FileInputStream(audioFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            mPlayOffset = 0;
            byte[] data_pack = null;
            if (inStream != null) {
                long size = audioFile.length();

                data_pack = new byte[(int) size];
                try {
                    inStream.read(data_pack);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
            }
            int bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            mPrimePlaySize = bufferSize * 2;
            while (mPlaying) {
                try {
                    int size = audioTrack.write(data_pack, mPlayOffset, mPrimePlaySize);
                    mPlayOffset += mPrimePlaySize;
                } catch (Exception e) {
                    // TODO: handle exception
                    audioTrack.stop();
                    e.printStackTrace();
                    break;
                }
                if (mPlayOffset >= data_pack.length) {
                    audioTrack.stop();
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            button_start_play.setEnabled(true);
            button_stop_paly.setEnabled(false);
            button_start_record.setEnabled(true);
            button_stop_record.setEnabled(false);
        }

        @Override
        protected void onPreExecute() {
            // stateView.setText("正在播放");
            button_start_record.setEnabled(false);
            button_stop_record.setEnabled(false);
            button_start_play.setEnabled(false);
            button_stop_paly.setEnabled(true);
        }
    }
}
