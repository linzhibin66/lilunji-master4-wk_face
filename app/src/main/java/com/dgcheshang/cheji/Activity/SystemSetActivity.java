package com.dgcheshang.cheji.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;

import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.Database.MyDatabase;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.FilesUtil;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;

import java.io.File;

/**
 *系统设置
 */

public class SystemSetActivity extends Activity implements View.OnClickListener{

    Context context=SystemSetActivity.this;
    AudioManager mAudioManager;
    private EditText edt_idcard;
    CheckBox cb_startface;
    SharedPreferences zdcssp;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_set);
        initView();
    }

    /**
     * 初始化布局
     * */
    private void initView() {
        zdcssp = context.getSharedPreferences("zdcs", Context.MODE_PRIVATE);
        edt_idcard = (EditText) findViewById(R.id.edt_idcard);//身份证号
        Button bt_clear = (Button) findViewById(R.id.bt_clear);//清除按钮
        Button bt_clearall = (Button) findViewById(R.id.bt_clearall);//清除所有
        cb_startface = (CheckBox) findViewById(R.id.cb_startface);//启动人脸验证
        //关闭自动模式；
        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        //音量控制,初始化定义
         mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //取得最大音量
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 取得当前亮度
        int normal = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        Log.e("TAG","屏幕亮度："+normal);
        //取得当前音量
        int syscurrenvolume= mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        View layout_back = findViewById(R.id.layout_back);
        SeekBar seekbarvolume = (SeekBar) findViewById(R.id.seekBar);//声音
        SeekBar seekbarlight = (SeekBar) findViewById(R.id.seekBar_light);//亮度
        // 进度条绑定当前亮度
        seekbarlight.setMax(255);
        seekbarlight.setProgress(normal);
        seekbarvolume.setMax(maxVolume);
        seekbarvolume.setProgress(syscurrenvolume);
        //声音监听
        seekbarvolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int tmpInt = seekBar.getProgress();
                // 当进度小于1时，设置成1，防止太小。
                if (tmpInt < 1) {
                    tmpInt = 1;
                }
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, tmpInt, 0);

            }
        });
        //亮度监听
        seekbarlight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 取得当前进度
                int tmpInt = seekBar.getProgress();
                Log.e("TAG","屏幕亮度滑动后："+tmpInt);
                // 当进度小于80时，设置成80，防止太黑看不见的后果。
                if (tmpInt < 80) {
                    tmpInt = 80;
                }
                ContentResolver resolver = getContentResolver();
                // 根据当前进度改变亮度
                Uri uri = android.provider.Settings.System
                        .getUriFor("screen_brightness");
                android.provider.Settings.System.putInt(resolver, "screen_brightness",
                        tmpInt);
                resolver.notifyChange(uri, null);
//

            }
        });
        cb_startface.setChecked(zdcssp.getBoolean("startface",true));

        cb_startface.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true){
                    NettyConf.startface=true;
                    SharedPreferences.Editor edit = zdcssp.edit();
                    edit.putBoolean("startface",true);
                    edit.commit();
                }else {
                    chooseDialog();

                }
            }
        });
        layout_back.setOnClickListener(this);
        bt_clear.setOnClickListener(this);
        bt_clearall.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_back://返回
                finish();
                break;

            case R.id.bt_clear://清除
                String idcard = edt_idcard.getText().toString().trim();//身份证号
                clearData(idcard);
                break;
            case R.id.bt_clearall://清除
                clearAllData();
                break;
        }
    }

    /**
     * 清除个人缓存
     *
     * @param idcard*/
    private void clearData(String idcard) {
        if(NettyConf.xystate==1){
            Speaking.in("请先登出学员");
        }else if(NettyConf.jlstate==1){
            Speaking.in("请先登出教练");
        }else {
            String[] params = {idcard};
            int num = DbHandle.deleteData("tsfrz", "sfzh = ?", params);
            if (NettyConf.debug) {
                Log.e("TAG", "清除身份信息数量：" + num);
            }
            if (num > 0) {
                Speaking.in("清除成功");
            } else {
                Speaking.in("未找到缓存");
            }
        }
    }

    /**
     * 清除所有缓存
     *
     * @param */
    private void clearAllData() {
        if(NettyConf.xystate==1){
            Speaking.in("请先登出学员");
        }else if(NettyConf.jlstate==1){
            Speaking.in("请先登出教练");
        }else {
            int num = DbHandle.deleteData("tsfrz", null, null);
            if (NettyConf.debug) {
                Log.e("TAG", "清除身份信息数量：" + num);
            }
//            if (num > 0) {
//                Speaking.in("清除成功");
//            } else {
//                Speaking.in("未找到缓存");
//            }
            File file = new File(NettyConf.jlyxy_picurl);
            File file2 = new File(NettyConf.px_picurl);
            //清除下载的比对照片
            FilesUtil.deleteAllFiles(file);
            //清除登录登出照片
            FilesUtil.deleteAllFiles(file2);
            Speaking.in("清除成功");
        }
    }


    /**
     * 选择是否继续退出
     * */
    private void chooseDialog() {
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(context);
        normalDialog.setTitle("提示");
        normalDialog.setMessage("关闭人脸验证会存在学时审核不通风险，确定关闭吗？");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NettyConf.startface=false;
                        SharedPreferences.Editor edit = zdcssp.edit();
                        edit.putBoolean("startface",false);
                        edit.commit();
                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cb_startface.setChecked(true);
                    }
                });
        // 显示
        normalDialog.setCancelable(false);
        normalDialog.show();
    }
}
