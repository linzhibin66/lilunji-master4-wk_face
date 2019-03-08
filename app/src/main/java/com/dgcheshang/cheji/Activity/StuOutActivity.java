package com.dgcheshang.cheji.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.camera.Test;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.serverreply.XydcR;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.haoxueche.cameralib.util.CameraWindowSize;
import com.haoxueche.mz200alib.activity.NfcActivity;
import com.haoxueche.mz200alib.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
/**
 *学员登出页面
 */

public class StuOutActivity extends NfcActivity implements View.OnClickListener{
    Context context =StuOutActivity.this;

    private String TAG="StuOutActivity";
    ImageView image_shuaka,image_shenfen,image_paizhao;
    SharedPreferences sp;
    TextView tv_bianhao,tv_idcard,tv_carlx,tv_stu_name;
    Dialog loading;
    LoadingTimer loadingTimer;
    Timer timer;
    SfrzR xyxx;//全局参数
    String picurl;
    private final Object mSync = new Object();
    //是否可以刷卡false不可以，true可以
    boolean isnfc=true;
    BroadcastReceiver receiver;//下载广播
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
           if(msg.arg1==2){
                //学员登出
                synchronized (mSync) {
                    Bundle data = msg.getData();
                    XydcR xydcr = (XydcR) data.getSerializable("xydcr");//学员登录成功后返回来的数据
                    handleOut(xydcr);
                }
            }else if(msg.arg1==5){
                //读卡获取学员信息
                Bundle data = msg.getData();
                xyxx = (SfrzR) data.getSerializable("xyxx");
                getXyxx(xyxx);

            }else if(msg.arg1==6){//uid
               //读取uid成功返回
                isnfc=false;
                String xyuid = msg.getData().getString("xyuid");
                String sql="select * from tsfrz where uuid=? and lx=?";
                String[] params={xyuid,"4"};
                ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
                if(list.size()==0){
                    Speaking.in("无此学员登录信息");
                    //继续刷卡
                    isnfc=true;
                }else{
                    image_shuaka.setBackgroundResource(R.mipmap.login_rid_xycard_y);
                    xyxx=list.get(0);
                    long sj = Long.valueOf(xyxx.getSj());//登录时间
                    byte theoryType = xyxx.getTheoryType();
                    long bdtime = ZdUtil.getLongTime();
                    long i = bdtime - sj;
                    long i1 = 6 * 3600 * 1000;
                    long i2 = 2 * 3600 * 1000;

                    if(theoryType==0){
                        if(i<i1){
                            //未满足4小时
                            chooseDialog("一","4");
                        }else {
                            NettyConf.xbh=xyxx.getTybh();
                            NettyConf.xydlsj=xyxx.getSj();
                            getXyxx(xyxx);
                        }
                    }else if(theoryType==1){
                        if(i<i2){
                            //未满足2小时
                            chooseDialog("四","2");
                        }else {
                            NettyConf.xbh=xyxx.getTybh();
                            NettyConf.xydlsj=xyxx.getSj();
                            getXyxx(xyxx);
                        }
                    }

                }
            }else if(msg.arg1==8){
                studentOut2();
            }else if(msg.arg1==9){
                //登出拍照
                studentOut1();

            }else if(msg.arg1==10){

               int stunum = msg.getData().getInt("stunum");
               if(stunum>0){
                   Speaking.in("学员登出成功，下一位");
               }else {
                   Speaking.in("学员已全部登出，教练员请签退");
                   finish();
               }
           }else if(msg.arg1==16){
               if(NettyConf.startface==true){
                   image_paizhao.setBackgroundResource(R.mipmap.login_face_y);
               }else {
                   image_paizhao.setBackgroundResource(R.mipmap.login_pz_y);
               }
               Bundle data = msg.getData();
               final String picurl=data.getString("pic");
               ZdUtil.sendZpsc2("129","0","18","",picurl);
               isnfc=true;
               image_shuaka.setBackgroundResource(R.mipmap.login_rid_xycard_n);

               if(NettyConf.startface==true){
                   image_paizhao.setBackgroundResource(R.mipmap.login_face_n);
               }else {
                   image_paizhao.setBackgroundResource(R.mipmap.login_pz_n);
               }
           }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stuout);
        NettyConf.handlersmap.put("stuout",handler);
        initView();

    }

    private void initView() {
        View layout_back = findViewById(R.id.layout_back);//返回按钮
        //学员登录布局
        image_shuaka = (ImageView) findViewById(R.id.image_shuaka);//刷卡图片
        image_shenfen = (ImageView) findViewById(R.id.image_shenfen);//身份图片
        image_paizhao = (ImageView) findViewById(R.id.image_paizhao);//拍照图片
        tv_bianhao = (TextView) findViewById(R.id.tv_bianhao);//学员编号
        tv_idcard = (TextView) findViewById(R.id.tv_idcard);//身份证号
        tv_stu_name = (TextView) findViewById(R.id.tv_stu_name);//姓名
        tv_carlx = (TextView) findViewById(R.id.tv_carlx);//车型
        if(NettyConf.startface==true){
            image_paizhao.setBackgroundResource(R.mipmap.login_face_n);
        }
        layout_back.setOnClickListener(this);

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
        }
    }

    /**
     * 登出拍照
     */
    private void studentOut1() {
        loading = LoadingDialogUtils.createLoadingDialog(context,"正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer,NettyConf.controltime);

//        ZdUtil.studentOut1();
        if(NettyConf.startface==false){
            Test.floatTakePicture4("stuout","stuout");
        }
       // Test.testTakePhoto(CameraWindowSize.WINDOW_SIZE_SMALL, true,"18","stuout");
    }

    private void studentOut2(){
        List<Tdata> list=ZdUtil.studentOut2();

        if (ZdUtil.pdNetwork() && NettyConf.constate == 1 && NettyConf.jqstate == 1){
            GatewayService.sendHexMsgToServer("serverChannel",list);
        }else{
            DbHandle.insertTdatas(list,6);
            //改变学员登出状态
            XydcR xr=new XydcR();
            xr.setJg(1);
            xr.setXybh(xyxx.getTybh());
            handleOut(xr);
        }

    }

    /**
     * 登出处理
     * */
    public void handleOut(XydcR xydcr){
        //取消加载动画
        if(loadingTimer!=null){
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }

        if(xydcr.getJg()==1){
            //学员登出成功
            ZdUtil.handleStudentOut(xydcr.getXybh());

        }else {
            //登出失败
            Speaking.in("学员登出失败");
            isnfc=true;
        }
        LoadingDialogUtils.closeDialog(loading);
    }

    /**
     * 读卡成功后获取学员信息
     * */
    public void getXyxx(SfrzR xyxx){
        NettyConf.xbh=xyxx.getTybh();
        //获取信息成功后显示身份信息
        tv_bianhao.setText(xyxx.getTybh());
        tv_idcard.setText(xyxx.getSfzh());
        tv_stu_name.setText(xyxx.getXm());
        tv_carlx.setText(xyxx.getCx());
        if(NettyConf.startface==true){
            //人脸验证
            commonXy2(xyxx,"stuout");
        }else {
            studentOut1();
        }

    }
    /**
     * 人脸识别通道
     * type 分为login和out
     * */
    public void commonXy2(final SfrzR xyxx,final String type){
        NettyConf.isback=false;
        final String zp = xyxx.getZp();//下载路径
        Log.e("TAG","学员下载图片路径："+zp);
        if(zp==null||zp.equals("")){
            Toast.makeText(context,"没有照片下载有效路径",Toast.LENGTH_SHORT).show();
            NettyConf.isback=true;
            return;
        }
        final String sfzh = xyxx.getSfzh();
        //判断文件夹是否存在
        RlsbUtil.isexistAndBuild(NettyConf.jlyxy_picurl);
        //学员原始照片路径
        final String xyzp=NettyConf.jlyxy_picurl+sfzh+".jpg";

        if(RlsbUtil.isFileExist(xyzp)==false){
//            String zp1=new String(ByteUtil.hexStringToByte(zp));
//            Log.e("TAG","学员下载图片路径："+zp1);
            //没有学员照片去下载
            downFile(zp,sfzh,xyzp,type);
        }else {
            //有学员照片直接抓拍验证
            rlsb(xyzp,sfzh, type);
        }

    }

    /**
     * 下载文件
     * */
    public void downFile(String url, final String sfzh, final String xyzp, final String type){
        //下载文件
        try {
            final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            // 设置下载路径和文件名
            request.setDestinationInExternalPublicDir("jlyxypic", sfzh+".jpg");
            request.setMimeType("image/jpeg");
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
                        Log.e("TAG","下载学员照片成功");
                        //下载完成操作，保存原照片 身份证号用来区别

                        if(RlsbUtil.isFileExist(xyzp)==true){
                            //照片存在
                            rlsb(xyzp, sfzh,type);

                        }else {
                            //照片不错在
                            Log.e("TAG","学员照片下载失败1");
                            Speaking.in("学员照片下载失败");
                        }

                    }else {
                        NettyConf.isback=true;
                        Log.e("TAG","下载学员照片失败2");
                        Speaking.in("学员照片下载失败");
                    }
                }
            };
            registerReceiver(receiver, filter);
        }catch (Exception ex){
            NettyConf.isback=true;
            Log.e("TAG","下载学员照片失败3");
            Speaking.in("学员照片下载失败");
        }

    }

    /**
     * 人脸识别成功后处理教练登录或教练登出
     * ishave_pic 判断是否有教练照片，有则无需保存特征值，无则保存特征值 true有照片，false没照片
     * */

    public void rlsb(final String xyzp, final String sfzh, final String type ){
        Speaking.in("正在人脸识别，请对准摄像头");
        //只有当原始照片保存成功才进行人脸识别
        Test.floatTakePicture(sfzh,type,"stuout");

    }

    /**
     * 选择是否继续退出
     * */
    private void chooseDialog(String bf,String time) {
            final AlertDialog.Builder normalDialog = new AlertDialog.Builder(context);
            normalDialog.setTitle("提示");
            normalDialog.setMessage("您当前第"+bf+"部分培训时长未满"+time+"小时，确定要登出吗？");
            normalDialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            NettyConf.xbh=xyxx.getTybh();
                            NettyConf.xydlsj=xyxx.getSj();
                            getXyxx(xyxx);
                        }
                    });
            normalDialog.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            //继续刷卡
                        }
                    });
            // 显示
            normalDialog.setCancelable(false);
            normalDialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除计时器
        if(timer!=null){
            timer.cancel();
        }
        NettyConf.handlersmap.remove("stuout");
    }


    @Override
    public void onDataRead(String data) {

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String intentActionStr = intent.getAction();// 获取到本次启动的action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intentActionStr)// NDEF类型
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intentActionStr)// 其他类型
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentActionStr)) {// 未知类型
            //在intent中读取Tag id
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] bytesId = tag.getId();// 获取id数组get

            String  cardNo = MessageUtil.bytesToHexString(bytesId).toUpperCase();
            if(isnfc==true){
                Message msg = new Message();
                msg.arg1=6;
                Bundle bundle = new Bundle();
                bundle.putString("xyuid",cardNo);
                msg.setData(bundle);
                handler.sendMessage(msg);

            }else {

            }
        }
    }

    /**
     * 控制返回键无效
     * */
    public boolean onKeyDown(int keyCode,KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK){
//            //这里重写返回键
//            return true;
            if(NettyConf.isback==true){
                finish();
            }
        }
        return false;

    }
}
