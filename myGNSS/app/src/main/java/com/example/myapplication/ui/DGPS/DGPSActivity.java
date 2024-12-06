package com.example.myapplication.ui.DGPS;

import static com.example.myapplication.MainActivity.if_auto_record;
import static com.example.myapplication.MainActivity.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.myapplication.R;
import com.example.myapplication.ui.Map.MapFragment;
import com.example.myapplication.ui.Spp.SppFragment;
import com.example.myapplication.ui.adapter.myFragmentAdapter;
import com.example.myapplication.ui.myviewpager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class DGPSActivity extends AppCompatActivity implements View.OnClickListener{
    public static myviewpager viewPager1;
    private TabLayout mTablayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dgpsactivity);
        initview();
        // 设置标题栏名称
        setTitle("Real-time Differentiation");
        // 设置全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        List<Fragment> myFragmentList = new ArrayList<>();
        List<String> title = new ArrayList<>();
        DgpsFragment fragment1 = new DgpsFragment();
        MapFragment fragment2 = new MapFragment();

        myFragmentList.add(fragment1);
        myFragmentList.add(fragment2);

        title.add("Real-time Differentiation");
        title.add("Map");

        myFragmentAdapter myfragmentadapter = new myFragmentAdapter(getSupportFragmentManager(), myFragmentList, title);
        viewPager1.setAdapter(myfragmentadapter);
        viewPager1.setOffscreenPageLimit(1);
        mTablayout.setupWithViewPager(viewPager1);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button record_Button = findViewById(R.id.btn_record3);
        if(if_auto_record){
            record_Button.setEnabled(false);
        }
        record_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 设置按钮为灰色并不可点击
                record_Button.setEnabled(false);
                // 开始倒计时
                showCountdownDialog(); // 5秒倒计时
//                record_Button.setBackgroundColor(Color.GRAY);
                // 使用 Handler 延迟执行
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 设置布尔变量 a 为 true
                        if_auto_record = true;
                        // 再延迟 1 秒
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 设置布尔变量 a 为 false
                                if_auto_record = false;
                                // 恢复按钮状态
                                record_Button.setEnabled(true);
                                Toast.makeText(getApplicationContext(),"record successfully", Toast.LENGTH_SHORT).show();
//                                record_Button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // 恢复原来的颜色
                            }
                        }, 1000); // 1 秒
                    }
                }, 5000); // 5 秒
            }
        });

        viewPager1.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onViewPageSelected(position);
                viewPager1.setSwipeable(position != 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void showCountdownDialog() {
        // 创建一个 AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("COUNTDOWN");
        builder.setMessage("countdown: " + 5 + "sec");
        builder.setCancelable(false); // 不可取消

        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();

        // 使用 CountDownTimer 进行倒计时
        new CountDownTimer(5 * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 更新对话框中的消息
                dialog.setMessage("countdown: " + (millisUntilFinished / 1000) + "sec");
            }

            @Override
            public void onFinish() {
                // 倒计时结束时的操作
//                dialog.setMessage("倒计时结束");
                dialog.dismiss();
//                // 恢复按钮状态
//                record_Button.setEnabled(true);
            }
        }.start();
    }
    private void initview(){
        viewPager1 = findViewById(R.id.dgps_vp);
        mTablayout = findViewById(R.id.dgps_tab_layout);

        TextView textspp = findViewById(R.id.textdgps);
        TextView textmap = findViewById(R.id.dgps_textmap);

        LinearLayout llspp = findViewById(R.id.dgps_ll1);
        LinearLayout llmap = findViewById(R.id.dgps_ll2);

    }
    private void onViewPageSelected(int position) {
        resetbuttonstate();

    }

    private void resetbuttonstate() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        test();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onClick(View v) {
        int id  = v.getId();
        if(id == R.id.dgps_ll1){
            viewPager1.setCurrentItem(0);
        }else {
            viewPager1.setCurrentItem(1);
        }
    }
}