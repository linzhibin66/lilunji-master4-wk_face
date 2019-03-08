package com.dgcheshang.cheji.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dgcheshang.cheji.Bean.database.StudentBean;
import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.camera.FileUtil;
import com.dgcheshang.cheji.camera.PhotoUtil;
import com.dgcheshang.cheji.camera.SharePreferUtil;

import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.haoxueche.cameralib.manager.ICameraManager;
import com.haoxueche.cameralib.managerProxy.NativeCameraManagerProxy;
import com.haoxueche.cameralib.util.CameraInfo;
import com.haoxueche.cameralib.util.CameraWindowSize;
import com.haoxueche.cameralib.util.ImageStatus;
import com.haoxueche.cameralib.util.ImageUtil;
import com.haoxueche.winterlog.L;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

import static com.haoxueche.cameralib.util.ImageStatus.STATUS_OUT_CAMERA_NOT_CONNECTED;


/**
 * 学员登录
 * */
public class LoginStudentActivity extends Activity implements View.OnClickListener{

    Context context=LoginStudentActivity.this;
    private String TAG="LoginStudentActivity";
    public static final int LOGIN_STU_SUCCESS = 1;
    ArrayList<StudentBean> studentlist;
    ListView listview;
    String yzmm;
    Dialog loading;
    TextView tv_coachname,loginnum;
    int isqiantui=0;//强退状态,0表示未强退，1表示强退
    private ICameraManager cameraManager;
    private final Object mSync = new Object();
    View layout_showphoto;
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==10){
                //强制登出验证返回结果
                int yzjg = msg.getData().getInt("yzjg");
                if(yzjg==0){
                    /*if(!ZdUtil.ispz){
                        //学员登出
                        ZdUtil.qzStuOut();
                    }else {
                        Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                        loading.cancel();
                    }*/
                    isqiantui=1;
                    myAdapter.notifyDataSetChanged();

                }else {
                    loading.cancel();
                    Speaking.in("密码验证失败");
                }
            }else if(msg.arg1==11){
                finish();
            }else if(msg.arg1==15){
                Bundle data = msg.getData();
                String picurl=data.getString("pic");
                ZdUtil.sendZpsc2("129","0","5","",picurl);
                Speaking.in("中间拍照成功");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_student);
        NettyConf.handlersmap.put("loginstudent",handler);
        initView();
    }

    /**
     * 初始化布局
     * */
    MyAdapter myAdapter;
    private void initView() {
        SharedPreferences coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE);//教练保存的数据
        View layout_back = findViewById(R.id.layout_back);
        listview = (ListView) findViewById(R.id.listview);//学员登陆列表
        Button bt_login = (Button) findViewById(R.id.bt_login);//登录
        Button bt_out = (Button) findViewById(R.id.bt_out);//登录
        Button bt_tongpai = (Button) findViewById(R.id.bt_tongpai);//统拍
        Button close = (Button) findViewById(R.id.close);//关闭
        Button pz = (Button) findViewById(R.id.pz);//拍照
        View layout_qzout = findViewById(R.id.layout_qzout);//强制登出
        tv_coachname = (TextView) findViewById(R.id.tv_coachname);//教练姓名
        loginnum = (TextView) findViewById(R.id.loginnum);//登录个数
        layout_showphoto = findViewById(R.id.layout_showphoto);//显示拍照框
        layout_showphoto.setVisibility(View.INVISIBLE);
        if(NettyConf.jlstate!=0){
            tv_coachname.setText(coachsp.getString("jlxm",""));
        }

        layout_qzout.setOnClickListener(this);
        layout_back.setOnClickListener(this);
        bt_login.setOnClickListener(this);
        bt_out.setOnClickListener(this);
        bt_tongpai.setOnClickListener(this);
        close.setOnClickListener(this);
        pz.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent();

        switch (view.getId()){
            case R.id.layout_back://返回
                finish();
                break;

            case R.id.bt_login://登录
                if(ZdUtil.canLogin()) {
                    intent.setClass(context, StuLoginActivity.class);
//                    startActivityForResult(intent, REQUEST_A);
                    startActivity(intent);
                }
                break;

            case R.id.bt_out://登出
                intent.setClass(context,StuOutActivity.class);
//                startActivityForResult(intent,REQUEST_B);
                startActivity(intent);
                break;

            case R.id.layout_qzout://强制登出
                showliuyanDialog();
                break;

            case R.id.bt_tongpai://统一拍照
                if(studentlist.size()>0){
                    preview(view);
                    layout_showphoto.setVisibility(View.VISIBLE);
                }else {
                    Toast.makeText(context,"暂无学员登录",Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.close://关闭
                cameraManager.release();
                layout_showphoto.setVisibility(View.INVISIBLE);
                break;
            case R.id.pz://拍照
                takePhoto(view);
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(NettyConf.debug){
            Log.e("TAG","onResume");
        }
        //显示登录列表
        studentlist = DbHandle.stuQuery();
        loginnum.setText(studentlist.size()+"");
        Collections.reverse(studentlist);
        myAdapter = new MyAdapter();
        listview.setAdapter(myAdapter);
    }

    /**
     * 已登录学员Adapter
     * */
    public class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if(studentlist!=null){
                return studentlist.size();
            }else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            ViewHodler viewHodler=null;
            if(convertView==null){
                convertView= LayoutInflater.from(context).inflate(R.layout.stulist2_item,null);
                viewHodler = new ViewHodler();
                viewHodler.tv_name = (TextView) convertView.findViewById(R.id.tv_name);//姓名
                viewHodler.bt_only_out = (Button) convertView.findViewById(R.id.bt_only_out);//登出按钮
                viewHodler.tv_idcard = (TextView) convertView.findViewById(R.id.tv_idcard);//身份证
                viewHodler.tv_logintime = (TextView) convertView.findViewById(R.id.tv_logintime);//登录时间
                viewHodler.bt_paizhao = (Button) convertView.findViewById(R.id.bt_paizhao);//拍照

                convertView.setTag(viewHodler);
            }else {
                viewHodler = (ViewHodler) convertView.getTag();
            }
            final StudentBean studentbean = studentlist.get(position);
            String xm = studentbean.getXm();
            if(isqiantui==0){
                viewHodler.bt_only_out.setVisibility(View.GONE);
            }else {
                viewHodler.bt_only_out.setVisibility(View.VISIBLE);
            }
            final String sfzh = studentbean.getSfzh();
            String tybh = studentbean.getTybh();
            String sj=studentbean.getSj();
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sj=sdf.format(new Date(Long.valueOf(sj)));
            viewHodler.tv_name.setText(xm);
            viewHodler.tv_idcard.setText(sfzh);
            viewHodler.tv_logintime.setText(sj);
            //退出按钮监听
            viewHodler.bt_only_out.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            //拍照
            viewHodler.bt_paizhao.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NettyConf.xbh=studentbean.getTybh();
                    ZdUtil.sendZpsc("129", "0", "19");
                }
            });
            return convertView;
        }
    }

    class ViewHodler{
        TextView tv_name;
        Button bt_only_out;
        Button bt_paizhao;
        TextView tv_idcard;
        TextView tv_logintime;

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
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
                    ZdUtil.matchPassword(4,yzmm);
                    builder.dismiss();
                }else {
                    Toast.makeText(context,"请输入登出密码",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 统一拍照dialog
     * */
    private void showTongpaiDialog(){
        final AlertDialog builder = new AlertDialog.Builder(this,R.style.CustomDialog).create(); // 先得到构造器
        builder.show();
        builder.getWindow().setContentView(R.layout.dialog_text);
        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);//解决不能弹出键盘
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_text, null);
        builder.getWindow().setContentView(view);
        TextView text_content = (TextView) view.findViewById(R.id.text_content);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        Button bt_cacnel = (Button) view.findViewById(R.id.bt_cacnel);
        Button bt_sure = (Button) view.findViewById(R.id.bt_sure);
        tv_title.setText("提示");
        text_content.setText("是否进行统一拍照？");

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
                //拍一张照上传
               // Test.testTakePhoto(CameraWindowSize.WINDOW_SIZE_SMALL, true,"5","loginstudent");
//                ZdUtil.sendZpsc("129", "0", "5");
                preview(view);
                layout_showphoto.setVisibility(View.VISIBLE);
                builder.dismiss();
            }
        });
    }
/**
 * 显示预览框
 * */
public void preview(View view) {
    int cameraId = SharePreferUtil.isTimingPhotoUseCamera0() ? CameraInfo
            .CAMERA_FACING_INSIDE : CameraInfo.CAMERA_FACING_OUT;
    boolean previewMirrored = (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) &&
            SharePreferUtil.isOutPhotoMirrorFlip();
    boolean cameraRotate;
    if (cameraId == CameraInfo.CAMERA_FACING_OUT) {
        cameraRotate = SharePreferUtil.isOutCameraRotate();
    } else {
        cameraRotate = SharePreferUtil.isInsideCameraRotate();
    }
    cameraManager = new NativeCameraManagerProxy();
    cameraManager.init(CameraWindowSize.WINDOW_SIZE_SMALL, cameraId, previewMirrored,
            cameraRotate).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<ICameraManager>() {


        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(ICameraManager iCameraManager) {
            //预览成功
        }

        @Override
        public void onError(Throwable e) {
            L.e(e);
            Toast.makeText(CjApplication.getInstance(), "相机预览出错: " + e.getMessage(), Toast
                    .LENGTH_SHORT).show();
        }
    });
}

    public void takePhoto(View view) {
        cameraManager.takePicture().map(new Function<byte[], ImageSaveInfo>() {
            @Override
            public ImageSaveInfo apply(byte[] bytes) throws Exception {
                ImageSaveInfo saveInfo = new ImageSaveInfo();
                if (ImageUtil.isErrorImage(bytes)) {
                    saveInfo.setStatus(STATUS_OUT_CAMERA_NOT_CONNECTED);
                }
                String photoNo = String.valueOf(System.currentTimeMillis() / 1000);
                //照片保存路径
                String path = FileUtil.DEFAULT_IMG_DIR + File.separator + photoNo + ".jpg";

                saveInfo.setData(PhotoUtil.savePhoto(path, bytes, ""));
                saveInfo.setPath(path);
                return saveInfo;
            }
        }).subscribe(new SingleObserver<ImageSaveInfo>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(ImageSaveInfo imageSaveInfo) {
                //照片数据
                switch (imageSaveInfo.getStatus()) {
                    case ImageStatus.STATUS_SUCC:
                        break;
                    case ImageStatus.STATUS_OUT_CAMERA_NOT_CONNECTED:
                        Toast.makeText(CjApplication.getInstance(), "外置摄像头故障，请检查相机线连接或通知维护人员修理设备",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
               // Toast.makeText(CjApplication.getInstance(), "照片保存成功：" + imageSaveInfo.toString(), Toast.LENGTH_SHORT).show();
                ZdUtil.sendZpsc2("129", "0", "5","",imageSaveInfo.getPath());
                cameraManager.release();
                layout_showphoto.setVisibility(View.INVISIBLE);
                Speaking.in("拍照成功");



            }

            @Override
            public void onError(Throwable e) {
                L.e(e);
                Toast.makeText(CjApplication.getInstance(), "相机拍照出错: " + e.getMessage(), Toast
                        .LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(cameraManager!=null){
            cameraManager.release();
        }
    }
}
