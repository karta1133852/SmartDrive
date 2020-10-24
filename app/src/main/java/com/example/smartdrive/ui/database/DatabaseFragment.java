package com.example.smartdrive.ui.database;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartdrive.R;
import com.example.smartdrive.instance.Phase;

import java.util.ArrayList;

public class DatabaseFragment extends Fragment {

    private DatabaseViewModel databaseViewModel;

    private View root;
    private Button btnSearchData;
    private Spinner spinnerIC, spinnerPhase;
    private RecyclerView listSection;
    private TextView txtInfo, txtName;
    private TextView[] txtCountdown;
    private LinearLayout layout1, layout2;
    private ImageView[][] imageView;

    int[][] greenIndex;
    final static int PANEL_COUNT = 2;
    ArrayAdapter<String> adapter;

    private Handler mHandler;
    boolean lightOn = false;
    boolean isYellowBlink = false, isRedBlink = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        databaseViewModel =
                ViewModelProviders.of(getActivity()).get(DatabaseViewModel.class);
        root = inflater.inflate(R.layout.fragment_database, container, false);

        mHandler = new Handler();
        greenIndex = new int[PANEL_COUNT][];

        bindView();
        setViewModel();
        setListener();

        startRepeatingTask();

        return root;
    }

    private void bindView() {

        btnSearchData = root.findViewById(R.id.btn_searchData);
        spinnerIC = root.findViewById(R.id.spinnerIC);
        spinnerPhase = root.findViewById(R.id.spinnerPhase);
        //listSection = root.findViewById(R.id.list_section);
        txtInfo = root.findViewById(R.id.txt_info);
        txtName = root.findViewById(R.id.txt_name);

        layout1 = root.findViewById(R.id.layout1_test);
        layout2 = root.findViewById(R.id.layout2_test);
        layout1.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);

        txtCountdown = new TextView[PANEL_COUNT];
        txtCountdown[0] = root.findViewById(R.id.txtCountdown1_test);
        txtCountdown[1] = root.findViewById(R.id.txtCountdown2_test);

        imageView = new ImageView[PANEL_COUNT][4];
        imageView[0][0] = root.findViewById(R.id.imgLight0_test);
        imageView[0][1] = root.findViewById(R.id.imgLight1_test);
        imageView[0][2] = root.findViewById(R.id.imgLight2_test);
        imageView[0][3] = root.findViewById(R.id.imgLight3_test);
        imageView[1][0] = root.findViewById(R.id.imgLight4_test);
        imageView[1][1] = root.findViewById(R.id.imgLight5_test);
        imageView[1][2] = root.findViewById(R.id.imgLight6_test);
        imageView[1][3] = root.findViewById(R.id.imgLight7_test);

        setLightGone(0);
        setLightGone(1);

        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, new String[] {"1", "2", "3", "4"});
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_item);
        spinnerPhase.setAdapter(adapter);
    }

    private void setViewModel() {
        databaseViewModel.getDeviceList().observe(this, new Observer<ArrayList<String>>() {
            @Override
            public void onChanged(@Nullable ArrayList<String> deviceList) {
                adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1, deviceList);
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_item);
                spinnerIC.setAdapter(adapter);
            }
        });
        databaseViewModel.getStatusStr().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                txtInfo.setText(s);
            }
        });
        databaseViewModel.getDeviceID().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                txtName.setText(s);
                spinnerIC.setSelection(adapter.getPosition(s));
            }
        });

        databaseViewModel.getPhases().observe(this, new Observer<Phase[]>() {
            @Override
            public void onChanged(Phase[] phases) {
                layout1.setVisibility(View.VISIBLE);
                for (int i = 0; i < PANEL_COUNT; i++) {
                    greenIndex[i] = new int[]{2};
                    setLightGone(i);
                }

                if (phases.length == 1) {
                    if (phases[0].countdown == -1) {
                        txtCountdown[0].setText("？");
                        return;
                    }
                    layout2.setVisibility(View.GONE);
                    if (phases[0].phaseOrder == null)
                        Log.d("ui", "phaseOrder null");
                    else if (phases[0].phaseOrder.equals("B0")) {
                        txtCountdown[0].setText("閃");
                        if (phases[0].phaseState.equals("閃黃燈")) {
                            isYellowBlink = true;
                            isRedBlink = false;
                        } else {
                            isYellowBlink = false;
                            isRedBlink = true;
                        }
                        return;
                    }
                } else {
                    layout2.setVisibility(View.VISIBLE);
                }

                isYellowBlink = false;
                isRedBlink = false;

                String phaseOrder = phases[0].phaseOrder;
                int planOrder = phases[0].planOrder;
                setPanelStatus(phaseOrder, planOrder);

                for (int i = 0; i < phases.length; i++) {
                    Phase phase = phases[i];
                    setLightGone(i);
                    setLightStatus(i, phase.phaseState);
                    txtCountdown[i].setText(phase.countdown+"");
                }
            }
        });
    }

    private void setListener() {

        btnSearchData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceID = spinnerIC.getSelectedItem().toString();
                txtName.setText(deviceID);
                //txtInfo.setText("");
                databaseViewModel.setDeviceID(deviceID);
                databaseViewModel.setPhase(spinnerPhase.getSelectedItem().toString());
            }
        });
    }

    private void setLightStatus(int index, String phaseState) {
        if (phaseState.equals("green")) {
            for (int i = 0; i < greenIndex[index].length; i++) {
                int greenNum = greenIndex[index][i];
                imageView[index][greenNum].setVisibility(View.VISIBLE);
            }
        } else if (phaseState.equals("yellow")) {
            imageView[index][1].setVisibility(View.VISIBLE);
        } else if (phaseState.equals("red")) {
            imageView[index][0].setVisibility(View.VISIBLE);
        }
    }

    private void setLightGone(int index) {
        for (int i = 0; i < imageView[index].length; i++) {
            imageView[index][i].setVisibility(View.GONE);
        }
    }

    private void setLightVisibility(int index, int visibility) {
        for (int i = 0; i < imageView[index].length; i++) {
            imageView[index][i].setVisibility(visibility);
        }
    }

    private void setPanelStatus (String phaseOrder, int planOrder) {
        if (phaseOrder.equals("00") || phaseOrder.equals("A0") || phaseOrder.equals("A2")) {
            setPanelNormal();
        } else if (phaseOrder.equals("40")) {
            if (planOrder == 0)
                setPanelStraightLeft();
            else
                setPanelNormal();
        } else if (phaseOrder.equals("60") || phaseOrder.equals("52")) {
            setPanelStraightLeft();
        } else if (phaseOrder.equals("D8")) {
            setPanelStraightLeft();
        } else if (phaseOrder.equals("E2")) {
            if (planOrder == 0)
                setPanelNormal();
            else if (planOrder == 1)
                setPanelStraightLeft();
            else
                setPanelStraightLeft();
        } else {
            // TODO 目前不支援的phaseOrder
            setPanelNormal();
        }
    }

    private void setPanelNormal() {
        imageView[0][2].setImageResource(R.drawable.green);
    }
    private void setPanelStraightLeft() {
        imageView[0][2].setImageResource(R.drawable.straight);
        imageView[0][3].setImageResource(R.drawable.right);
        imageView[1][2].setImageResource(R.drawable.left);
        greenIndex[0] = new int[] {2, 3};
    }


    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {
            if (isYellowBlink || isRedBlink) {
                setLightVisibility(0, View.GONE);
                int index;
                if (isYellowBlink) {
                    index = 1;
                } else {
                    index = 0;
                }
                if (lightOn)
                    imageView[0][index].setVisibility(View.GONE);
                else
                    imageView[0][index].setVisibility(View.VISIBLE);
                lightOn = !lightOn;
            }
            mHandler.postDelayed(mHandlerTask, 1000);
        }
    };
    void startRepeatingTask() {
        mHandlerTask.run();
    }
    void stopRepeatingTask() {
        mHandler.removeCallbacks(mHandlerTask);
    }
}