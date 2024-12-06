package com.example.myapplication.ui.RTK;

import static com.example.myapplication.MainActivity.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;

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
import com.example.myapplication.ui.adapter.myFragmentAdapter;
import com.example.myapplication.ui.myviewpager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class RTKActivity extends AppCompatActivity implements View.OnClickListener{
    public static myviewpager viewPager1;
    private List<Fragment> myFragmentList;
    private myFragmentAdapter myfragmentadapter;
    private TabLayout mTablayout;
    private List<String> Title;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rtkactivity);
        viewPager1 = findViewById(R.id.rtk_vp);
        mTablayout = findViewById(R.id.rtk_tab_layout);
//        strName = findViewById(R.id.str_name);
//        strNameValue = findViewById(R.id.str_name_value);
//        strName.setText("1");
//        strNameValue.setText("1");
        setTitle("Real-time Kinematic");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        myFragmentList = new ArrayList<>();
        Title = new ArrayList<>();
        RtkFragment fragment1 = new RtkFragment();
        MapFragment fragment2 = new MapFragment();

        myFragmentList.add(fragment1);
        myFragmentList.add(fragment2);

        Title.add("Real-time Kinematic");
        Title.add("Map");

        myfragmentadapter = new myFragmentAdapter(getSupportFragmentManager(), myFragmentList, Title);
        viewPager1.setAdapter(myfragmentadapter);
        viewPager1.setOffscreenPageLimit(1);
        mTablayout.setupWithViewPager(viewPager1);
//        Button record_Button = findViewById(R.id.btn_record);
//        if(if_auto_record){
//            record_Button.setEnabled(false);
//        }
//        record_Button.setOnClickListener(v -> {
//            // 获取 EditText 中的数字
//            String input = strNameValue.getText().toString();
//            if (!input.isEmpty()) {
//                int number = Integer.parseInt(input); // 将字符串转换为整数
//                number += 1; // 加1
//                strNameValue.setText(String.valueOf(number)); // 更新 EditText 的值
//            }
//            // 设置按钮为不可点击
//            record_Button.setEnabled(false);
//            if_auto_record = true;
//            // 再延迟 1 秒
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    // 设置布尔变量 a 为 false
//                    if_auto_record = false;
//                    record_Button.setEnabled(true);
//                    // 恢复按钮状态
//                    record_Button.setEnabled(true);
//                    Toast.makeText(getApplicationContext(),"record successfully", Toast.LENGTH_SHORT).show();
////                                record_Button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // 恢复原来的颜色
//                }
//            }, 1000); // 1 秒
////                if_ave_result=true;
////                // 设置按钮为灰色并不可点击
////                record_Button.setEnabled(false);
////                // 开始倒计时
////                showCountdownDialog(); // 5秒倒计时
//////                record_Button.setBackgroundColor(Color.GRAY);
////                // 使用 Handler 延迟执行
////                new Handler().postDelayed(new Runnable() {
////                    @Override
////                    public void run() {
////                        // 设置布尔变量 a 为 true
////                        if_auto_record = true;
////                        if_ave_result=false;
////                        // 再延迟 1 秒
////                        new Handler().postDelayed(new Runnable() {
////                            @Override
////                            public void run() {
////                                // 设置布尔变量 a 为 false
////                                if_auto_record = false;
////                                // 恢复按钮状态
////                                record_Button.setEnabled(true);
////                                Toast.makeText(getApplicationContext(),"record successfully", Toast.LENGTH_SHORT).show();
//////                                record_Button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // 恢复原来的颜色
////                            }
////                        }, 1000); // 1 秒
////                    }
////                }, 5000); // 5 秒
//        });



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
    private void onViewPageSelected(int position) {
        resetbuttonstate();

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
        if(id == R.id.ll1){
            viewPager1.setCurrentItem(0);
        }else {
            viewPager1.setCurrentItem(1);
        }
    }
}