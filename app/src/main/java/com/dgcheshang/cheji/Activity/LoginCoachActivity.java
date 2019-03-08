package com.dgcheshang.cheji.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.camera.Test;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Jlydl;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.serverreply.JlydcR;
import com.dgcheshang.cheji.netty.serverreply.JlydlR;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.haoxueche.cameralib.util.CameraWindowSize;
import com.haoxueche.mz200alib.activity.NfcActivity;
import com.haoxueche.mz200alib.util.MessageUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

/**
 * 教练员登录
 * */
public class LoginCoachActivity extends NfcActivity implements View.OnClickListener {
    public String TAG="LoginCoachActivity";
    Context context=LoginCoachActivity.this;
    public static final int LOGIN_COA_SUCCESS = 0;
    public static final int REQUEST_A = 1;

    ImageView image_shuaka,image_shenfen,image_project;
    TextView tv_shenfen,tv_jlbh,tv_chexin,tv_coach_name,tv_kechen;
    View layout_shenfen;
    private TextView tv_title;
    SharedPreferences.Editor editor;
    Dialog loading;
    LoadingTimer loadingTimer;
    Timer timer;
    SfrzR jlxx;
    String yzmm;
    //读卡：true可以读卡，false不可以
    boolean isnfc=true;
    private final Object mSync = new Object();
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){//教练登录
                synchronized (mSync) {
                    handleIn(msg);
                }
            }else if(msg.arg1==2){//教练员登出
                synchronized (mSync) {
                    Bundle data = msg.getData();
                    JlydcR jldcr = (JlydcR) data.getSerializable("jlydcr");//教练登录成功后返回来的数据
                    handleOut(jldcr);
                }
            }else if(msg.arg1==5){//获取教练信息
                Bundle data = msg.getData();
                jlxx = (SfrzR) data.getSerializable("jlxx");
                if(jlxx.getJg()==0){
                    getJlxx(jlxx);
                }else {
                    image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_n);
                    Speaking.in("无效卡");
                    isnfc=true;
                }

            }else if(msg.arg1==6){//uid
                isnfc=false;
                String jluid = msg.getData().getString("jluid");

               /* String sql="select * from tsfrz where uuid=? and lx=?";
                String[] params={jluid,"1"};
                MyDatabase myDatabase = new MyDatabase(context);
                ArrayList<SfrzR> list= myDatabase.queryTsfrz(sql,params);
                if(list.size()==0){*/
                if(ZdUtil.pdNetwork()&&NettyConf.constate==1) {
                    image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_y);
                    ZdUtil.sendSfrz(jluid, "1");
                }else {
                    Speaking.in("请检查网络并连接服务器");
                }
                /*}else{
                    jlxx=list.get(0);
                    getJlxx(jlxx);
                }*/
            }else if(msg.arg1==7){
                //指纹验证成功返回登录
                loading = LoadingDialogUtils.createLoadingDialog(context, "正在登录...");
                coachLogin();
            }else if(msg.arg1==8){
                coachOut2();
            }else if(msg.arg1==9){
                //指纹匹配成功登出
                coachOut1();
            }else if (msg.arg1==10){
                //强制登出返回回来处理
                int yzjg = msg.getData().getInt("yzjg");
                if(yzjg==0){
                    editor.putString("yzmm",yzmm);//保存验证密码
                    editor.commit();
                    if(!ZdUtil.ispz){
                        if (NettyConf.xystate==1){
                            Toast.makeText(context,"请先登出学员！",Toast.LENGTH_SHORT).show();
                        }else {
                            coachOut1();
                        }
                    }else {
                        Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    loading.cancel();
                    Speaking.in("密码验证失败");
                }
            }else if(msg.arg1==11){
                //启动刷卡
                Speaking.in("教练员请刷卡");
                isnfc=true;
            }else if(msg.arg1==15){
                Bundle data = msg.getData();
                String picurl=data.getString("pic");
                ZdUtil.sendZpsc2("129","0","20","",picurl);
                Speaking.in("教练员登录成功");
                Intent intent = new Intent();
                setResult(LOGIN_COA_SUCCESS,intent);
                LoadingDialogUtils.closeDialog(loading);
                finish();
            }else if(msg.arg1==16){
                Bundle data = msg.getData();
                String picurl=data.getString("pic");
                ZdUtil.sendZpsc2("129","0","21","",picurl);
                isnfc=true;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("TAG","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_coach);
        NettyConf.handlersmap.put("logincoach",handler);
        initView();
        if(NettyConf.jlstate!=1) {
//            Speaking.in("请选择培训课程");
            Speaking.in("教练员请刷卡");
            isnfc=true;
        }
    }

    /**
     *
     * 初始化布局
     * */
    private void initView() {
        //保存教练信息
        SharedPreferences coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE); //私有数据
        editor = coachsp.edit();//获取编辑器
        View layout_back = findViewById(R.id.layout_back);//返回
        tv_title = (TextView) findViewById(R.id.tv_title);//标题
        //登录页面布局
        View layout_coachin = findViewById(R.id.layout_coachin);//登录布局
        layout_shenfen = findViewById(R.id.layout_shenfen);//身份信息布局
        layout_shenfen.setVisibility(View.INVISIBLE);
        tv_kechen = (TextView) findViewById(R.id.tv_kechen);//选择课程显示
        Button bt_choose = (Button) findViewById(R.id.bt_choose);//课程选择按钮
        image_shuaka = (ImageView) findViewById(R.id.image_shuaka);//刷卡图片
        image_project = (ImageView) findViewById(R.id.image_project);//课堂图片
        image_shenfen = (ImageView) findViewById(R.id.image_shenfen);//身份验证图片
        tv_shenfen = (TextView) findViewById(R.id.tv_shenfen);//身份证号
        tv_jlbh = (TextView) findViewById(R.id.tv_jlbh);//教练编号
        tv_coach_name = (TextView) findViewById(R.id.tv_coach_name);//教练姓名
        tv_chexin = (TextView) findViewById(R.id.tv_chexin);//车型
        //登出页面布局
        View layout_coachout = findViewById(R.id.layout_coachout);//登出布局
        Button bt_coachout = (Button) findViewById(R.id.bt_coachout);//登出按钮
        TextView tv_coachcode = (TextView) findViewById(R.id.tv_coachcode);//教练编号
        TextView tv_coachzj = (TextView) findViewById(R.id.tv_coachzj);//证件号码
        TextView tv_cartype = (TextView) findViewById(R.id.tv_cartype);//车牌类型
        TextView tv_coachname = (TextView) findViewById(R.id.tv_coachname);//教练姓名
        TextView tv_logintime = (TextView) findViewById(R.id.tv_logintime);//教练姓名

        View layout_qzout = findViewById(R.id.layout_qzout);//强制登出
        //判断是否教练登录过
        if(NettyConf.jlstate==1){//登录过
            layout_coachin.setVisibility(View.GONE);
            layout_coachout.setVisibility(View.VISIBLE);
            layout_qzout.setVisibility(View.VISIBLE);
            tv_title.setText("教练员管理");
            tv_coachcode.setText(NettyConf.jbh);
            tv_cartype.setText(NettyConf.cx);
            tv_coachzj.setText(NettyConf.jzjhm);
            String jlxm = coachsp.getString("jlxm", "");
            tv_coachname.setText(jlxm);
            String xzkc = coachsp.getString("xzkc", "");//选择的课程
            tv_kechen.setText(xzkc);
            long dlsj = coachsp.getLong("dlsj", 0l);
            if(dlsj!=0){
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String format = sf.format(new Date(dlsj));
                tv_logintime.setText(format);
            }


        }else {//没登录
            layout_coachin.setVisibility(View.VISIBLE);
            layout_coachout.setVisibility(View.GONE);
            layout_qzout.setVisibility(View.GONE);
        }

        layout_back.setOnClickListener(this);
        bt_coachout.setOnClickListener(this);
        layout_qzout.setOnClickListener(this);
        bt_choose.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.layout_back://返回
                if(isnfc==true){
                    finish();
                }

                break;

            case R.id.bt_coachout://登出
                if(!ZdUtil.ispz){
                    long l= DbHandle.getNum("select count(*) from stulogin",null);
                    if (l>0){
                        Toast.makeText(context,"请先登出所有学员！",Toast.LENGTH_SHORT).show();
                    }else {
                        isnfc=false;
                        coachOut();
                    }
                }else {
                    Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_qzout://强制退出
                showliuyanDialog();
                break;

            case R.id.bt_choose://课程选择
                Choosedialog();
                break;
        }
    }


    /**
     * 第几部分选择
     * */
    private void Choosedialog(){
        final String items[]={"第一部分","第四部分"};
        final String[] pxnr = {"1"};
        AlertDialog.Builder builder=new AlertDialog.Builder(context);  //先得到构造器
        builder.setTitle("部分选择"); //设置标题
        builder.setSingleChoiceItems(items,0,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.dismiss();
                if( items[which].equals("第一部分")){
                    pxnr[0] ="1";
                }else {
                    pxnr[0] ="4";
                }
            }
        });
        builder.setPositiveButton("确定",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
//                Toast.makeText(context, "确定"+pxnr[0], Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setClass(context, ObjectContent1Activity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("objecttype",pxnr[0]);
                startActivityForResult(intent,REQUEST_A);
            }
        });
        builder.create().show();
    }

    /**
     * 课程选择完后跳转回来
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){

            case REQUEST_A:

                switch (resultCode){
                    case ObjectContent1Activity.LOGIN_CONTENT_SUCCESS:
                        String pxnr = data.getStringExtra("pxnr");//培训内容
                        String objecttype = data.getStringExtra("objecttype");//培训第几部分
                        String kcbh = data.getStringExtra("kcbh");
                        if(objecttype.equals("1")){
                            tv_kechen.setText("第一部分——"+pxnr);
                            editor.putString("xzkc","第一部分——"+pxnr);
                            editor.commit();
                        }else if(objecttype.equals("4")){
                            tv_kechen.setText("第四部分——"+pxnr);
                            editor.putString("xzkc","第四部分——"+pxnr);
                            editor.commit();
                        }
                        if(NettyConf.debug){
                            Log.e("TAG","培训部分:"+objecttype);
                            Log.e("TAG","培训项目:"+kcbh);
                        }
                        String pxkc="200"+objecttype+kcbh+"0000";//培训课程
                        NettyConf.pxkc=pxkc;
                        editor.putString("pxck",pxkc);//保存培训课程
                        editor.commit();//
                        //选择完成后改变课程选择图片
                        image_project.setBackgroundResource(R.mipmap.login_project_y);
                        //启动刷卡
                        Message msg = new Message();
                        msg.arg1=11;
                        handler.sendMessage(msg);


                        break;
                }
                break;
        }
    }
    /**
     * 教练登录
     * */
    private void coachLogin() {
    try {
            Jlydl jlydl = new Jlydl();
            jlydl.setJlybh(NettyConf.jbh);//教练员编号
            jlydl.setJlyzjhm(NettyConf.jzjhm);
            jlydl.setZjcx(NettyConf.cx);//车型
            jlydl.setPxkc("0000000000");
            long bdtime = ZdUtil.getLongTime();
            String ktid = String.valueOf(bdtime);
            ktid = ktid.substring(ktid.length() - 12, ktid.length() - 3);
            NettyConf.ktid = ktid;
            jlydl.setKtid(ktid);
            editor.putString("ktid",ktid);//课堂id
            editor.putLong("dlsj",bdtime);//登录时间
            editor.commit();
            if(NettyConf.debug){
                Log.e("TAG","学员登陆培训课程:"+NettyConf.pxkc);
                Log.e("TAG","学员登陆课堂ID:"+NettyConf.ktid);
            }
            byte[] b3 = jlydl.getJlydlbytes();
            byte[] b2 = MsgUtilClient.getMsgExtend(b3, "0101", "13", "2");
            List<Tdata> list = MsgUtilClient.generateMsg(b2, "0900", NettyConf.mobile, "1");

            if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1) {
                if(NettyConf.debug){
                    Log.e("TAG"+TAG,"发送教练数据");
                }
                GatewayService.sendHexMsgToServer("serverChannel",list);
            }else{
                Speaking.in("连接已断开");
            }

        }catch(Exception e){
            Toast.makeText(context,"教练员登陆数据异常",Toast.LENGTH_SHORT).show();
            isnfc=true;
        }
    }

    /**
     * 教练登出
     * */
    private void coachOut() {
        String sql="select * from tsfrz where tybh=? and lx=?";
        String[] params={NettyConf.jbh,"1"};
        ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
        if(list.size()==0){
            coachOut1();
        }else{
            SfrzR jlxx=list.get(0);
            getJlyxxOut(jlxx);
        }
    }

    /**
     * 教练登出
     * */
    private void coachOut1() {
        loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer, NettyConf.controltime);
        ZdUtil.coachOut1();
    }

    private void coachOut2() {
        try {
            List<Tdata> list=ZdUtil.coachOut2();

            if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1){
                GatewayService.sendHexMsgToServer("serverChannel",list);
            }else{
                DbHandle.insertTdatas(list,7);
                JlydcR jr=new JlydcR();
                jr.setJg(1);
                handleOut(jr);
            }

        }catch(Exception e){
            Toast.makeText(context,"教练员登出数据异常",Toast.LENGTH_SHORT).show();
            isnfc=true;
        }
    }

    /**
     * 登录处理
     * */
    public void handleIn(Message msg){
        if(loadingTimer!=null) {
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }

        Bundle data = msg.getData();
        final JlydlR jldlr = (JlydlR) data.getSerializable("jldlr");//教练登录成功后返回来的数据

        if(jldlr.getJg()==1){//教练登录成功
            image_shenfen.setBackgroundResource(R.mipmap.login_ok_y);
            editor.putString("jlbh", NettyConf.jbh);//教练编号
            editor.putInt("jlstate",1);
            editor.putString("cx",NettyConf.cx);//教练车型
            editor.putString("jzjhm",NettyConf.jzjhm);//证件号码
            editor.putString("jlxm",jlxx.getXm());//教练姓名
            editor.putString("ktid",NettyConf.ktid);//教练姓名
            Date d=new Date();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            editor.putString("dlday",sdf.format(d));//教练姓名
            editor.commit();//提交修改

            if(NettyConf.jlstate!=1){
                NettyConf.jlstate=1;
                //上传拍照数据
//                ZdUtil.sendZpsc("129","0","20");
                Test.floatTakePicture4("jllogin","logincoach");
               // Test.testTakePhoto(CameraWindowSize.WINDOW_SIZE_SMALL, true,"20","logincoach");
            }

            //登录成功延时返回主页，防止拍照被关闭
//            final Timer dlystimer = new Timer();
//            TimerTask dlystask=new TimerTask() {
//                @Override
//                public void run() {
//                    dlystimer.cancel();
//                    Speaking.in(jldlr.getFjxx());
//                    Intent intent = new Intent();
//                    setResult(LOGIN_COA_SUCCESS,intent);
//                    LoadingDialogUtils.closeDialog(loading);
//                    finish();
//                }
//            };
//            dlystimer.schedule(dlystask,2000);

        }else {
            LoadingDialogUtils.closeDialog(loading);
            Log.e("TAG","登录失败报读："+jldlr.getFjxx());
            Speaking.in(jldlr.getFjxx());
            isnfc=true;

        }
    }

    /**
     * 登出处理
     * */
    public void handleOut(JlydcR jldcr){
        if(loadingTimer!=null){
            loadingTimer.cancel();
        }
        if(timer!=null){
            timer.cancel();
        }

        if(jldcr.getJg()==1){
            Speaking.in("教练员登出成功");
            isnfc=true;
            if(NettyConf.jlstate!=0) {
                NettyConf.jlstate = 0;
            }
            editor.putInt("jlstate",0);
            editor.commit();

            Intent intent = new Intent();
            setResult(LOGIN_COA_SUCCESS,intent);

            LoadingDialogUtils.closeDialog(loading);

            finish();
        }else {
            Speaking.in("教练员登出失败");
            isnfc=true;
        }
    }

    /**
     * 读卡成功后获取教练信息
     * */
    public void getJlxx(SfrzR jlxx){
        NettyConf.cx = jlxx.getCx();//车型
        NettyConf.jbh = jlxx.getTybh();//统一编号
        NettyConf.jzjhm = jlxx.getSfzh();//身份证号

        //获取信息成功后显示身份信息
        layout_shenfen.setVisibility(View.VISIBLE);
        tv_shenfen.setText(jlxx.getSfzh());
        tv_chexin.setText(jlxx.getCx());
        tv_jlbh.setText(jlxx.getTybh());
        tv_coach_name.setText(jlxx.getXm());
        coachLogin();
    }

    /**
     * 教练登出
     */
    public void getJlyxxOut(SfrzR jlxx){
        String xx = jlxx.getXx();//教练指纹
//        if(StringUtils.isNotEmpty(xx)){
//            commonCoach(xx,"jlcardout");
//        }else{
//            coachOut1();
//        }
        coachOut1();
    }


    /**
     * 关闭页面调用
     * */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除计时器
        if(timer!=null){
            timer.cancel();
        }
        NettyConf.handlersmap.remove("logincoach");
    }

    /**
     * 强制登出dialog
     *
     * */

    private void showliuyanDialog(){
        final AlertDialog builder = new AlertDialog.Builder(this,R.style.CustomDialog).create(); // 先得到构造器
        builder.show();
        builder.getWindow().setContentView(R.layout.dialog_appoint_edt);
        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);//解决不能弹出键盘
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_appoint_edt, null);
        builder.getWindow().setContentView(view);
        final EditText edt_content = (EditText) view.findViewById(R.id.edt_content);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        Button bt_cacnel = (Button) view.findViewById(R.id.bt_cacnel);
        Button bt_sure = (Button) view.findViewById(R.id.bt_sure);
        tv_title.setText("登出验证");

        //取消
        bt_cacnel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });

        //确定
        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 yzmm = edt_content.getText().toString().trim();
                if(!yzmm.equals("")){
                    builder.dismiss();
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
                    ZdUtil.matchPassword(1,yzmm);


                }else {
                    Toast.makeText(context,"请输入登出密码",Toast.LENGTH_SHORT).show();
                }
            }
        });
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
           if(NettyConf.jlstate==0&&isnfc==true){
               Message msg = new Message();
               msg.arg1=6;
               Bundle bundle = new Bundle();
               bundle.putString("jluid",cardNo);
               msg.setData(bundle);
               handler.sendMessage(msg);

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
