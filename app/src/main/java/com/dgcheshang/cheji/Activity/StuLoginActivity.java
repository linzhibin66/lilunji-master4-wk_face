package com.dgcheshang.cheji.Activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
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

import com.dgcheshang.cheji.Bean.database.StudentBean;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.CarTypeUtil;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.camera.Test;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.po.Xydl;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.serverreply.XydlR;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.haoxueche.cameralib.util.CameraWindowSize;
import com.haoxueche.mz200alib.activity.NfcActivity;
import com.haoxueche.mz200alib.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 *学员登录页面
 */

public class StuLoginActivity extends NfcActivity implements View.OnClickListener{

    Context context=StuLoginActivity.this;
    private String TAG="StuLoginActivity";
    ImageView image_shuaka,image_shenfen,image_paizhao;

    SharedPreferences sp;
    TextView tv_bianhao,tv_idcard,tv_carlx,tv_stu_name;
    SharedPreferences.Editor editor;
    Dialog loading;
    LoadingTimer loadingTimer;
    Timer timer;
    SfrzR xyxx;//全局参数
    String picurl;
    private final Object mSync = new Object();
    //是否可以刷卡false不可以，true可以
    boolean isnfc=true;
    BroadcastReceiver receiver;//下载广播
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){
                //学员登录
                synchronized (mSync) {
                    handleIn(msg);
                }
            }else if(msg.arg1==5){
                //读卡获取学员信息
                Bundle data = msg.getData();
                xyxx = (SfrzR) data.getSerializable("xyxx");
                if(xyxx.getJg()==0){
                    //判断是否已经登录过
                    String[] params={xyxx.getSfzh()};
                    ArrayList<StudentBean> studentlst = DbHandle.queryStuxx("select * from stulogin where sfzh=?", params);
                    if(NettyConf.debug){
                        Log.e("TAG","登陆的学员个数："+studentlst.size());
                    }
                    if(studentlst.size()>0){
                        image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_n);
                        Speaking.in("此学员已登录");
                        isnfc=true;
                    }else {
                        getXyxx(xyxx);
                    }
                }else {
                    image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_n);
                    Speaking.in("无效卡");
                    isnfc=true;
                }

            }else if(msg.arg1==6){//uid
                String xyuid = msg.getData().getString("xyuid");
                image_shuaka.setBackgroundResource(R.mipmap.login_rid_xycard_y);
                isnfc=false;
                /*String sql="select * from tsfrz where uuid=? and lx=?";
                String[] params={xyuid,"4"};
                MyDatabase myDatabase = new MyDatabase(context);
                ArrayList<SfrzR> list= myDatabase.queryTsfrz(sql,params);
                if(list.size()==0){*/
                if(ZdUtil.pdNetwork()&& NettyConf.constate==1) {
                    ZdUtil.sendSfrz(xyuid,"4");
                }else {
                    Speaking.in("服务器已断开");
                }
                /*}else{
                    xyxx=list.get(0);
                    getXyxx(xyxx);
                }*/
            }else if(msg.arg1==15){
                image_paizhao.setBackgroundResource(R.mipmap.login_pz_y);
                Bundle data = msg.getData();
                String picurl=data.getString("pic");
                ZdUtil.sendZpsc2("129","0","17","",picurl);
                isnfc=true;
                image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_n);
                if(NettyConf.startface==true){
                    image_paizhao.setBackgroundResource(R.mipmap.login_face_n);
                }else {
                    image_paizhao.setBackgroundResource(R.mipmap.login_pz_n);
                }
                Speaking.in("学员登陆成功,下一位");
            }else if(msg.arg1==17){
                if(NettyConf.startface==true){
                    image_paizhao.setBackgroundResource(R.mipmap.login_face_y);
                }else {
                    image_paizhao.setBackgroundResource(R.mipmap.login_pz_y);
                }
                Bundle data = msg.getData();
                picurl=data.getString("pic");
                studentLogin();
                isnfc=true;
            }
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stulogin);
        NettyConf.handlersmap.put("stulogin",handler);
        initView();
    }

    private void initView() {
        View layout_back = findViewById(R.id.layout_back);
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
     * 按钮监听
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
     * 学员登录
     * */
    private void studentLogin() {
        try {
            loading = LoadingDialogUtils.createLoadingDialog(context, "正在登录...");
            Xydl xydl = new Xydl();
            xydl.setXybh(NettyConf.xbh);//学员编号
            xydl.setJlbh(NettyConf.jbh);//教练编号
            xydl.setKtid(NettyConf.ktid);//课堂id
            xydl.setPxkc(NettyConf.pxkc);//培训课程
            if(NettyConf.debug){
                Log.e("TAG","学员登陆培训课程:"+NettyConf.pxkc);
                Log.e("TAG","学员登陆课堂ID:"+NettyConf.ktid);
            }
            byte[] xydlb3 = xydl.getXydlBytes();
            byte[] xydlb2 = MsgUtilClient.getMsgExtend(xydlb3, "0201", "13", "2");
            List<Tdata> list = MsgUtilClient.generateMsg(xydlb2, "0900", NettyConf.mobile, "1");

            if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1) {
                GatewayService.sendHexMsgToServer("serverChannel",list);
            }else{
                Speaking.in("服务已断开");
            }

        }catch (Exception e){
            Log.e(TAG,"学员登陆数据异常:"+e.getMessage());
            Toast.makeText(context,"学员登陆数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 登录处理
     * */
    public void handleIn(Message msg){
        //取消加载动画
        if(loadingTimer!=null) {
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }

        Bundle data = msg.getData();
        XydlR xydlr = (XydlR) data.getSerializable("xydlr");//学员登录成功后返回来的数据
        if(xydlr.getJg()==1){//学员登录成功

            //上传拍照数据
//            ZdUtil.sendZpsc("129", "0", "17");
            if(NettyConf.startface==false){
                Test.floatTakePicture4("stulogin","stulogin");
            }else {
                ZdUtil.sendZpsc2("129","0","17","",picurl);
                image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_n);
                image_paizhao.setBackgroundResource(R.mipmap.login_pz_n);
                Speaking.in("学员登陆成功,下一位");
            }
            //Test.testTakePhoto(CameraWindowSize.WINDOW_SIZE_SMALL, true,"17","stulogin");
            //保存学员信息到数据库中
            StudentBean studentbean = new StudentBean();
            studentbean.setXm(xyxx.getXm());
            studentbean.setSfzh(xyxx.getSfzh());
            studentbean.setCx(xyxx.getCx());
            studentbean.setTybh(xyxx.getTybh());
            DbHandle.insertStuData(studentbean);
            DbHandle.insertTsfrz(xyxx);


        }else {
            Log.e("TAG","登录失败:"+xydlr.getFjxx());
            Speaking.in(xydlr.getFjxx());
            //置空数据
            tv_bianhao.setText("");
            tv_idcard.setText("");
            tv_stu_name.setText("");
            tv_carlx.setText("");

        }
        //关闭提示
        LoadingDialogUtils.closeDialog(loading);

    }


    /**
     * 读卡成功后获取学员信息
     * */
    public void getXyxx(SfrzR xyxx){
        //获取信息成功后显示身份信息
        tv_bianhao.setText(xyxx.getTybh());
        tv_idcard.setText(xyxx.getSfzh());
        tv_stu_name.setText(xyxx.getXm());
        String cx = xyxx.getCx();
        tv_carlx.setText(cx);
        NettyConf.xbh=xyxx.getTybh();
        String cx1="";
        String cartypeNum = CarTypeUtil.getCartypeNum(cx);
        byte theoryType = xyxx.getTheoryType();//第几部分
        if(theoryType==0){
            cx1="2"+cartypeNum+"103";
        }else if(theoryType==1){
            cx1="2"+cartypeNum+"448";
        }
        NettyConf.pxkc=cx1+"0000";

        if(NettyConf.startface==true){
            //开启人脸验证
            commonXy2(xyxx,"stulogin");
        }else {
            studentLogin();
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
        Test.floatTakePicture(sfzh,type,"stulogin");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除计时器
        if(timer!=null){
            timer.cancel();
        }
        NettyConf.handlersmap.remove("stulogin");
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
