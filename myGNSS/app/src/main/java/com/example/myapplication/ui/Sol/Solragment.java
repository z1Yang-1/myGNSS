package com.example.myapplication.ui.Sol;

import static android.content.Context.MODE_PRIVATE;
import static com.example.myapplication.MainActivity.Cn0DbHz;
import static com.example.myapplication.MainActivity.auto_record_mode;
import static com.example.myapplication.MainActivity.checked;
import static com.example.myapplication.MainActivity.if_auto_record;
import static com.example.myapplication.MainActivity.if_setting;
import static com.example.myapplication.ui.Sol.SolViewModel.prcopt;
import static com.example.myapplication.ui.measure_util.prcopt_default;
import static com.example.myapplication.ui.measure_util.solopt_default;
import static com.rtklib.rtkpos.rtkPos.D2R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.ui.Spp.SppViewModel;
import com.example.myapplication.ui.Spp.SppViewModelFactory;
import com.example.myapplication.ui.status.HomeViewModel;
import com.rtklib.bean.prcopt_t;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Solragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Solragment extends Fragment {
    private CheckBox checkBox_bds, checkBox2_gps, checkBox3_glo, checkBox4_gal;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private SolViewModel solViewModel;
    private Spinner iono_spinner;
    private Spinner tropo_spinner;
    private Spinner azel;
    private Spinner ambres;
    private Spinner ambres_bds;
    private Spinner ambres_glo;
    private Spinner cn0;
    private Spinner Freq;
    private RadioGroup record_format;
    private RadioGroup record_mode;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch switchPseudoRange;
    EditText editText;
    private static final String PREFS_NAME_SETTING = "solSetting";
    private Button save_btn;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Solragment() {
        // Required empty public constructor
    }

    public static Solragment newInstance(String param1, String param2) {
        Solragment fragment = new Solragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        // 保存用户选择到 SharedPreferences
        SharedPreferences sharedPref = requireActivity().getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("iono_selection", iono_spinner.getSelectedItemPosition()); // 保存iono_spinner的选择
        editor.putInt("tropo_selection", tropo_spinner.getSelectedItemPosition()); // 保存tropo_spinner的选择
        editor.putInt("azel_selection", azel.getSelectedItemPosition());
        editor.putInt("cn0_selection", cn0.getSelectedItemPosition());
        editor.putInt("ambres_selection", ambres.getSelectedItemPosition());
        editor.putInt("ambres_selection_bds", ambres_bds.getSelectedItemPosition());
        editor.putInt("ambres_selection_glo", ambres_glo.getSelectedItemPosition());
        editor.apply();
    }
    @Override
    public void onResume() {
        super.onResume();
        // 恢复用户选择
        SharedPreferences sharedPref = requireActivity().getPreferences(MODE_PRIVATE);
        int ionoSelection = sharedPref.getInt("iono_selection", 0); // 默认值为0
        int tropoSelection = sharedPref.getInt("tropo_selection", 0); // 默认值为0
        int azelSelection = sharedPref.getInt("azel_selection", 0); // 默认值为0
        int cn0Selection = sharedPref.getInt("cn0_selection", 0); // 默认值为0
        int ambresSelection = sharedPref.getInt("ambres_selection", 0);
        int ambresSelection_bds = sharedPref.getInt("ambres_selection_bds", 0);
        int ambresSelection_glo = sharedPref.getInt("ambres_selection_glo", 0);
        // 设置Spinner的选择
        iono_spinner.setSelection(ionoSelection);
        tropo_spinner.setSelection(tropoSelection);
        azel.setSelection(azelSelection);
        ambres.setSelection(ambresSelection);
        ambres_bds.setSelection(ambresSelection_bds);
        ambres_glo.setSelection(ambresSelection_glo);
        cn0.setSelection(cn0Selection);
    }
    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SolViewModel.open = false;
        prcopt.ionoopt=0;
        prcopt.tropopt=0;
        solViewModel = new ViewModelProvider(this).get(SolViewModel.class);
        View view = inflater.inflate(R.layout.fragment_solragment, container, false);
        iono_spinner = view.findViewById(R.id.ionosphere_model_spinner);
        tropo_spinner = view.findViewById(R.id.troposphere_model_spinner);
        azel = view.findViewById(R.id.azel_spinner);
        cn0 = view.findViewById(R.id.CN0_Threshold_spinner);
        ambres = view.findViewById(R.id.ambiguity_Res_spinner_GPS);
        ambres_bds = view.findViewById(R.id.ambiguity_Res_spinner_BDS);
        ambres_glo = view.findViewById(R.id.ambiguity_Res_spinner_GLO);
        Freq = view.findViewById(R.id.Frequency_spinner);
        editText = view.findViewById(R.id.min_fix_res_value);
        switchPseudoRange = view.findViewById(R.id.switch_pseudo_range);
        save_btn = view.findViewById(R.id.btn_save_setting);
        record_format = view.findViewById(R.id.radioGroup);
        record_mode = view.findViewById(R.id.radioGroup_record);
        // 初始化 CheckBox
        checkBox_bds = view.findViewById(R.id.checkBox);
        checkBox2_gps = view.findViewById(R.id.checkBox2);
        checkBox3_glo = view.findViewById(R.id.checkBox3);
        checkBox4_gal = view.findViewById(R.id.checkBox4);
        solViewModel.solSettingPrefer = requireContext().getSharedPreferences(PREFS_NAME_SETTING, MODE_PRIVATE);
        String savedValue = solViewModel.solSettingPrefer.getString("inputValue", "");
        // 读取保存的值
        editText.setText(savedValue);
        // 读取状态并设置 CheckBox
        checkBox_bds.setChecked(solViewModel.solSettingPrefer.getBoolean("BDS", false));
        checkBox2_gps.setChecked(solViewModel.solSettingPrefer.getBoolean("GPS", true));
        checkBox3_glo.setChecked(solViewModel.solSettingPrefer.getBoolean("GALILEO", false));
        checkBox4_gal.setChecked(solViewModel.solSettingPrefer.getBoolean("GLONASS", false));
        // 更新 a 的值
        updateValue();
        // 设置 CheckBox 的点击事件
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> updateValue();
        checkBox_bds.setOnCheckedChangeListener(listener);
        checkBox2_gps.setOnCheckedChangeListener(listener);
        checkBox3_glo.setOnCheckedChangeListener(listener);
        checkBox4_gal.setOnCheckedChangeListener(listener);
        // 读取上一次的选择
        int selectedRadioButtonId = solViewModel.solSettingPrefer.getInt("record_format", -1);
        int selectedRadioButtonId1 = solViewModel.solSettingPrefer.getInt("record_mode", -1);
        // 设置上一次的选择
        if (selectedRadioButtonId != -1) {
            record_format.check(selectedRadioButtonId);
            if (selectedRadioButtonId == R.id.radioButtonXYZ) {
                solopt_default.posf = 1;
            } else if (selectedRadioButtonId == R.id.radioButtonBLH) {
                solopt_default.posf = 0;
            }
        }else{
            //如果是第一次使用APP，进行默认设置
            RadioButton radioButtonAutomatic = record_format.findViewById(R.id.radioButtonBLH);
            radioButtonAutomatic.setChecked(true);
            solopt_default.posf = 0;
        }
        if (selectedRadioButtonId1 != -1) {
            record_mode.check(selectedRadioButtonId1);
            // 根据选中的单选按钮设置 if_auto_record
            if (selectedRadioButtonId1 == R.id.radioButtonAutomatic) {
                if_auto_record = true;
                auto_record_mode = true;
            } else if (selectedRadioButtonId1 == R.id.radioButtonManual) {
                if_auto_record = false;
                auto_record_mode = false;
            }
        }else{
            RadioButton radioButtonAutomatic = record_mode.findViewById(R.id.radioButtonAutomatic);
            radioButtonAutomatic.setChecked(true);
            if_auto_record = true;
            auto_record_mode = false;
        }
        // 创建适配器并设置数据
        // 使用正确的构造函数来创建 ArrayAdapter 对象
        ArrayAdapter<String> tropo_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.troposphere_model);
        ArrayAdapter<String> iono_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.ionosphere_model);
        ArrayAdapter<String> azel_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.azel);
        ArrayAdapter<String> ambres_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.ambrestest);
        ArrayAdapter<String> ambres_adapter_bds = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.ambres_bds);
        ArrayAdapter<String> ambres_adapter_glo = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.ambres_glo);
        ArrayAdapter<String> cn0_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.cn0);
        ArrayAdapter<String> Freq_adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, solViewModel.Freq);
        iono_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tropo_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        azel_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ambres_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ambres_adapter_bds.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ambres_adapter_glo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cn0_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Freq_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 将适配器设置给 Spinner
        iono_spinner.setAdapter(iono_adapter);
        tropo_spinner.setAdapter(tropo_adapter);
        azel.setAdapter(azel_adapter);
        ambres.setAdapter(ambres_adapter);
        ambres_glo.setAdapter(ambres_adapter_glo);
        ambres_bds.setAdapter(ambres_adapter_bds);
        cn0.setAdapter(cn0_adapter);
        Freq.setAdapter(Freq_adapter);
        // Inflate the layout for this fragment
        return view;
    }
    private void updateValue() {
        // 根据选中的 CheckBox 更新 a 的值
        //a = 0; // 重置 a 的值
        for (int i = 0; i < 4; i++) {
            checked[i] = false;
        }
        if (checkBox2_gps.isChecked()) { // GPS
            checked[0] = true;
            //a += 1;
        }
        if (checkBox_bds.isChecked()) { // BDS
            //a += 2;
            checked[1] = true;
        }
        if (checkBox4_gal.isChecked()) { // GLONASS
            //a += 4;
            checked[2] = true;
        }
        if (checkBox3_glo.isChecked()) { // GALILEO
            //a += 8;
            checked[3] = true;
        }
    }
    private void saveCheckboxState() {
        SharedPreferences.Editor editor = solViewModel.solSettingPrefer.edit();
        editor.putBoolean("BDS", checkBox_bds.isChecked());
        editor.putBoolean("GPS", checkBox2_gps.isChecked());
        editor.putBoolean("GALILEO", checkBox3_glo.isChecked());
        editor.putBoolean("GLONASS", checkBox4_gal.isChecked());
        editor.apply(); // 提交更改
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchPseudoRange.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SolViewModel.open = isChecked;
        });
        record_mode.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = solViewModel.solSettingPrefer.edit();
            editor.putInt("record_mode", checkedId);
            editor.apply();
            if (checkedId == R.id.radioButtonAutomatic) {
                if_auto_record = true;
                auto_record_mode = true;
            } else if (checkedId == R.id.radioButtonManual) {
                if_auto_record = false;
                auto_record_mode = false;
            }
        });
        record_format.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = solViewModel.solSettingPrefer.edit();
            editor.putInt("record_format", checkedId);
            editor.apply();
            if (checkedId == R.id.radioButtonXYZ) {
                solopt_default.posf = 1;
            } else if (checkedId == R.id.radioButtonBLH) {
                solopt_default.posf = 0;
            } else {
                solopt_default.posf = 0;
            }
        });
        iono_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 当选择了 Spinner 中的某个选项时触发的事件
                Log.d("SelectedPosition", String.valueOf(position)); // 获取选中的选项在 Spinner 中的位置
                if(position==0){
                    prcopt.ionoopt=0;
                }else if(position==1){
                    prcopt.ionoopt=1;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 当没有选择任何选项时触发的事件
                Log.d("SelectedPosition", "Nothing selected");
            }
        });
        tropo_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 当选择了 Spinner 中的某个选项时触发的事件
                Log.d("SelectedPosition", String.valueOf(position)); // 获取选中的选项在 Spinner 中的位置
                if(position==0){
                    prcopt.tropopt=0;
                }else if(position==1){
                    prcopt.tropopt=1;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 当没有选择任何选项时触发的事件
                Log.d("SelectedPosition", "Nothing selected");
            }
        });
        azel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 当选择了 Spinner 中的某个选项时触发的事件
                Log.d("SelectedPosition", String.valueOf(position)); // 获取选中的选项在 Spinner 中的位置
                if (position >= 0 && position <= 18) {
                    prcopt.setElmin((position * 5) * D2R);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 当没有选择任何选项时触发的事件
                Log.d("SelectedPosition", "Nothing selected");
            }
        });
        cn0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 当选择了 Spinner 中的某个选项时触发的事件
                Log.d("SelectedPosition", String.valueOf(position)); // 获取选中的选项在 Spinner 中的位置
                if (position >= 0 && position <= 9) {
                    Cn0DbHz = position*5;
//                    prcopt.setElmin((position * 5) * D2R);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 当没有选择任何选项时触发的事件
                Log.d("SelectedPosition", "Nothing selected");
            }
        });
        ambres.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 当选择了 Spinner 中的某个选项时触发的事件
                Log.d("SelectedPosition", String.valueOf(position)); // 获取选中的选项在 Spinner 中的位置
                if(position==0){
                    prcopt.setModear(0);
                }else if(position==1){
                    prcopt.setModear(1);
                }else if(position==2){
                    prcopt.setModear(2);
                }else if(position==3){
                    prcopt.setModear(3);
                }else{
                    prcopt.setModear(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 当没有选择任何选项时触发的事件
                Log.d("SelectedPosition", "Nothing selected");
            }
        });
        // 设置点击事件
        save_btn.setOnClickListener(v -> {
            // 在这里处理按钮点击事件
            if_setting = true;
            saveCheckboxState();
            String inputValue = editText.getText().toString();
            if(!TextUtils.isEmpty(inputValue)){
                prcopt.setThresar(new double[]{Float.parseFloat(inputValue), 0.9999, 0.25, 0.1, 0.05});
            }else{

            }
            // 保存输入的值
            Toast.makeText(requireContext(),"saved",Toast.LENGTH_SHORT).show();
        });
        editText.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                String inputValue = editText.getText().toString();
                prcopt.setThresar(new double[]{Float.parseFloat(inputValue), 0.9999, 0.25, 0.1, 0.05});
                SharedPreferences.Editor editor = solViewModel.solSettingPrefer.edit();
                editor.putString("inputValue", inputValue);
                editor.apply();
            }
        });
    }
}