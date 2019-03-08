package com.dgcheshang.cheji.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.dgcheshang.cheji.Bean.VersionBean;
import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.Tools.update.DownLoadApk;
import com.dgcheshang.cheji.camera.SharePreferUtil;
import com.dgcheshang.cheji.camera.Test;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.init.ZdClient;
import com.dgcheshang.cheji.netty.thread.SpeakThread;
import com.dgcheshang.cheji.netty.timer.CacheTimer;
import com.dgcheshang.cheji.netty.timer.LoginoutTimer;
import com.dgcheshang.cheji.netty.timer.LoginoutWarnTimer;
import com.dgcheshang.cheji.netty.timer.OutTimer;
import com.dgcheshang.cheji.netty.util.InitUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.dgcheshang.cheji.networkUrl.NetworkUrl;
import com.google.gson.Gson;
import com.haoxueche.cameralib.manager.ICameraManager;
import com.haoxueche.cameralib.managerProxy.NativeCameraManagerProxy;
import com.haoxueche.cameralib.util.CameraInfo;
import com.haoxueche.cameralib.util.CameraWindowSize;
import com.haoxueche.mz200alib.util.InstallUtil;
import com.haoxueche.winterlog.L;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 主菜单
 * */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    public static LoginActivity instance = null;

    Context context = LoginActivity.this;

    private static final String TAG = "MainActivity";
    public static final int REQUEST_COACH = 1;//跳转教练登录页面
    public static final int REQUEST_STUDENT = 2;//跳转学员登录页面
    public static final int REQUEST_SETTING = 3;//跳转设置ip,端口页面
    private ICameraManager cameraManager;
    Dialog loading;
    TextView tv_coach_state,tv_student_state;
    private final Object mSync = new Object();
    String fileurl="/sdcard/APPdown";//下载文件夹路径
    BroadcastReceiver receiver;//下载广播
    NetworkReceiver networkReceiver;//网络监听广播
    View layout_showphoto;
    SoundPool soundPool;
    public static boolean hyconstate=false;//华盈连接标志

    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {

            if(msg.arg1==1) {//注销
                handleCancel(msg);
            }else if(msg.arg1==3){

            }else if(msg.arg1==5){
                handleshow();
            }else if(msg.arg1==6){
                Bundle data = msg.getData();
                String url= (String) data.getSerializable("url");
                downFile(url);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        new Thread(new SpeakThread()).start();

        NettyConf.handlersmap.put("login",handler);
        //初始化
        InitUtil.initSystem();
//        loading = LoadingDialogUtils.createLoadingDialog(context, "正在初始化...");
        initView();
        //广播
        registerReceiver();
        instance = this;

        //拍照秒提示嘀嘀声初始化
        soundPool= new SoundPool(10, AudioManager.STREAM_SYSTEM,5);
        soundPool.load(CjApplication.getInstance(), R.raw.didi4,1);

        //清除缓存数据
        CacheTimer cacheTimer=new CacheTimer();
        new Timer().schedule(cacheTimer,0,24*60*60*1000);

        //是否启动强制退出
        new Timer().schedule(new OutTimer(),20);

        //启动强制登出定时器
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date date=new Date();//取时间
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//        calendar.add(calendar.DATE,1);//把日期往后增加一天.整数往后推,负数往前移动
//        date=calendar.getTime();
//        StringBuffer sb=new StringBuffer(sdf.format(date).substring(0,11));
//        sb.append("00:00:00");
//        try {
//            if(NettyConf.debug){
//                Log.e("TAG","登出计时器触发时间:"+sb.toString());
//            }
//            //String hs="2017-07-17 15:08:20";
//            Date d=sdf.parse(sb.toString());
//            LoginoutTimer lt=new LoginoutTimer();
//            new Timer().schedule(lt,d,24*60*60*1000);
//
//            calendar.setTime(d);
//            calendar.add(Calendar.MINUTE, -5);
//
//            Date d2=calendar.getTime();
//            if(NettyConf.debug){
//                Log.e("TAG","登出报警计时器触发时间:"+sdf.format(d2));
//            }
//            LoginoutWarnTimer lwt=new LoginoutWarnTimer();
//
//            //String hs2="2017-07-17 15:08:00";
//            new Timer().schedule(lwt,d2,24*60*60*1000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        //测试摄像头
        if(NettyConf.ispz==false){
            Test.floatTakePicture5(getWindow().getDecorView());
        }

    }

    /**
     * 初始化布局
     * */
    private void initView() {
        View layout_back = findViewById(R.id.layout_back);//返回
        View layout_coach = findViewById(R.id.layout_coach);//教练
        View layout_student = findViewById(R.id.layout_student);//学员
        tv_coach_state = (TextView) findViewById(R.id.tv_coach_state);//教练显示状态
        tv_student_state = (TextView) findViewById(R.id.tv_student_state);//学员显示状态
        View layout_cardetail = findViewById(R.id.layout_cardetail);//车辆信息
        View layout_about = findViewById(R.id.layout_about);//关于我们
        View layout_basic_set = findViewById(R.id.layout_basic_set);//基本设置
        View layout_setting = findViewById(R.id.layout_setting);//参数设置
        layout_showphoto = findViewById(R.id.layout_showphoto);//显示拍照框
        layout_showphoto.setVisibility(View.INVISIBLE);
        layout_back.setOnClickListener(this);
        layout_coach.setOnClickListener(this);
        layout_student.setOnClickListener(this);
        layout_cardetail.setOnClickListener(this);
        layout_about.setOnClickListener(this);
        layout_setting.setOnClickListener(this);
        layout_basic_set.setOnClickListener(this);
        layout_showphoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout_showphoto.setVisibility(View.INVISIBLE);
                //关闭摄像头
                releaseCamera();
                return true;
            }
        });
    }


    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.layout_back://摄像头
                layout_showphoto.setVisibility(View.VISIBLE);
                preview(getWindow().getDecorView());
                break;

            case R.id.layout_coach://教练员管理
                if(NettyConf.jlstate==1||ZdUtil.canLogin()) {
                    intent.setClass(context, LoginCoachActivity.class);
                    startActivityForResult(intent, REQUEST_COACH);
                }
                break;

            case R.id.layout_student://学员管理
                if(ZdUtil.canLogin()) {
                    if (NettyConf.jlstate == 0) {
                        Toast.makeText(context, "请先教练员登录", Toast.LENGTH_SHORT).show();
                    } else {
                        intent.setClass(context, LoginStudentActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                }
                break;

            case R.id.layout_basic_set://基本设置
                intent.setClass(context,SystemSetActivity.class);
                startActivityForResult(intent, REQUEST_SETTING);
                break;

            case R.id.layout_setting://设置
                intent.setClass(context,MainActivity.class);
                startActivityForResult(intent, REQUEST_SETTING);
                break;


            case R.id.layout_cardetail://车辆信息
                intent.setClass(context,CarDetailActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.layout_about://关于我们
                intent.setClass(context,AboutActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

        }
    }
    /**
     * 跳转页面返回回来结果处理
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){
            case REQUEST_COACH://教练页面返回来
                switch (resultCode){
                    case LoginCoachActivity.LOGIN_COA_SUCCESS://教练返回回来

                        break;
                }
                break;

            case REQUEST_STUDENT://学员页面返回来
                switch (resultCode){
                    case LoginStudentActivity.LOGIN_STU_SUCCESS://学员返回回来

                        break;
                }
                break;

        }
    }

    /**
     * 控制返回键无效
     * */
    public boolean onKeyDown(int keyCode,KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
//            //这里重写返回键
//            return true;
        }
        return false;
    }


    /**
     * 注销返回处理
     *
     * @param msg*/
    public void handleCancel(Message msg){

        Bundle data = msg.getData();
        int zxjg = (int) data.get("zxjg");
        if(zxjg==1){//注销成功
            //清除定位计时器
            Object o=NettyConf.timermap.get("wzhb");
            if(o!=null){
                Timer timer= (Timer) o;
                timer.cancel();
            }
            //清除定位服务
            o=NettyConf.servicemap.get("wzhb");
            if(o!=null){
                Intent intent= (Intent) o;
                stopService(intent);
            }
            NettyConf.zcstate=0;//改变注册状态
            NettyConf.jqstate=0;//改变鉴权状态
            //清除保存状态
            SharedPreferences jianquan = getSharedPreferences("jianquan", Context.MODE_PRIVATE);
            Intent intent = new Intent();
            intent.setClass(context,MainActivity.class);
            jianquan.edit().clear();
            startActivity(intent);
            finish();
        }else {
            //注销失败
        }
    }

    public void handleshow(){
        if(NettyConf.jlstate==1){
            tv_coach_state.setText("教练员管理(已登录)");
        }else {
            tv_coach_state.setText("教练员管理(未登录)");
        }
    }

    /**
     * app获取版本,是否需要更新
     * */
    public void getVersion( ) {
        StringRequest request = new StringRequest(Request.Method.POST, NetworkUrl.UpdateCodeUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Gson gson = new Gson();
                    VersionBean versionbean = gson.fromJson(response, VersionBean.class);

                    //管理员卡uid
                    if(!versionbean.getManageruid().equals("")){
                        //保存管理员卡号
                        SharedPreferences uidsp = getSharedPreferences("uid", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = uidsp.edit();
                        if(NettyConf.debug){
                            Log.e("TAG",versionbean.getManageruid());
                        }
                        edit.putString("uid",versionbean.getManageruid());
                        edit.commit();
                    }
                    //判断是否版本一致
                    if (Double.valueOf(versionbean.getVersion())>Double.valueOf(NettyConf.version)) {
                        //进行版本更新
//                        updateDialog(versionbean.getUrl(), versionbean.getMsg());
                        if(versionbean.getImei().equals("")){
                            //全部更新
                            downFile(versionbean.getUrl());
                        }else {
                            //个别更新
                            String imei = versionbean.getImei();
                            String[] split = imei.split(",");
                            for (int i=0; i<split.length;i++){
                                if(split[i].equals(NettyConf.imei)){
                                    downFile(versionbean.getUrl());
                                    return;
                                }

                            }
                        }
                    }
                }catch (Exception e){

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("TGA","volleyError="+volleyError);
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                return map;
            }
        };
        CjApplication.getHttpQueue().add(request);
    }


    /**
     * 版本更新提示
     *
     * @param url
     * @param msg*/
    private void updateDialog(final String url, final String msg){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);  //先得到构造器
        builder.setTitle("版本提示"); //设置标题
        builder.setMessage("有新版本更新，是否更新？"); //设置内容
        builder.setPositiveButton("下载", new DialogInterface.OnClickListener() { //设置确定按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show();
                DownLoadApk.download(context,url,"驾培车机下载","cheshang");
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() { //设置取消按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //参数都设置完成了，创建并显示出来//不可按返回键取消
        builder.setCancelable(false).create().show();
    }

    /**
     * 下载文件
     * */
    public void downFile(String url){
        loading = LoadingDialogUtils.createLoadingDialog(context, "版本更新中...");
        File  destDir = new File(fileurl);
        //先判断是否有之前下载的文件，有则删除，
        if (!destDir.exists()) {
            destDir.mkdirs();
        }else {
            File[] files = destDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()){
                    File appfile = new File(files[i].getPath());
                    appfile.delete();
                }
            }
            destDir.mkdirs();
        }
        final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置下载路径和文件名
        request.setDestinationInExternalPublicDir("APPdown", "cheji.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDescription("培训系统app正在下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        request.setAllowedOverRoaming(false);
        // 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        // 设置为可见和可管理
         request.setVisibleInDownloadsUi(true);
        // 获取此次下载的ID
         final long refernece = dManager.enqueue(request);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                       long myDwonloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (refernece == myDwonloadID) {
//                                Intent install = new Intent(Intent.ACTION_VIEW);
//                                Uri downloadFileUri = dManager.getUriForDownloadedFile(refernece);
//                                 install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
//                                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(install);
                                updateApp();
                               }
                        }
              };
         registerReceiver(receiver, filter);
        }

    /**
     * 自动升级
     * */
    public void updateApp(){
        InstallUtil.install(context,NettyConf.fileurl+"/cheji.apk");
    }

    /**
     * 注册网络监听广播
     * */

    private  void registerReceiver(){
        IntentFilter filter=new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkReceiver = new NetworkReceiver();
        this.registerReceiver(networkReceiver, filter);
    }

    /**
     * 网络广播
     * */
    public class NetworkReceiver extends BroadcastReceiver {
        boolean shenji=false;
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            boolean state;

            if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
                state=false;
            }else{
                state=true;
                if(shenji==false){
                    getVersion();//获取版本更新
                    shenji=true;
                }
            }

            if (NettyConf.netstate == null) {
                NettyConf.netstate = state;
                if(state){
                    ZdUtil.conServer();
                }
            } else if (NettyConf.netstate != state) {
                NettyConf.netstate = state;
                //如果网络变化
                if (state) {
                    ZdUtil.conServer();
                } else {
                    if (ZdClient.conTimer != null) {
                        ZdClient.conTimer.cancel();
                        ZdClient.conTimer = null;
                    }

                    NettyConf.constate = 0;
                    NettyConf.jqstate = 0;
                    Speaking.in("网络已断开");
                }
            }

        }
    }

    public void preview(View view) {
        int cameraId = SharePreferUtil.isTimingPhotoUseCamera0() ? CameraInfo
                .CAMERA_FACING_INSIDE : CameraInfo.CAMERA_FACING_OUT;
        boolean previewMirrored = (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) &&
                SharePreferUtil.isOutPhotoMirrorFlip();
        final boolean cameraRotate;
        if (cameraId == CameraInfo.CAMERA_FACING_OUT) {
            cameraRotate = SharePreferUtil.isOutCameraRotate();
        } else {
            cameraRotate = SharePreferUtil.isInsideCameraRotate();
        }
        releaseCamera();
        final ICameraManager iCameraManager = new NativeCameraManagerProxy();
        iCameraManager.init(CameraWindowSize.WINDOW_SIZE_SMALL, cameraId, previewMirrored,
                cameraRotate).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<ICameraManager>() {


            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(ICameraManager iCameraManager) {
                //预览成功
                cameraManager = iCameraManager;
            }

            @Override
            public void onError(Throwable e) {
                L.e(e);
                Toast.makeText(CjApplication.getInstance(), "相机预览出错: " + e.getMessage(), Toast
                        .LENGTH_SHORT).show();
                if(iCameraManager != null) {
                    iCameraManager.release();
                }
            }
        });
    }


    private void releaseCamera() {
        if(cameraManager != null) {
            cameraManager.release();
            cameraManager = null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        handleshow();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        Log.e("TAG", "onDestroy:");

        //解绑广播
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        if(networkReceiver!=null){
            unregisterReceiver(networkReceiver);
        }
        if(loading!=null){
            loading.cancel();
        }
        super.onDestroy();
    }

}
