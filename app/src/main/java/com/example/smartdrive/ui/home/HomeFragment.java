package com.example.smartdrive.ui.home;

import android.location.Location;
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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.smartdrive.R;
import com.example.smartdrive.instance.Direction;
import com.example.smartdrive.instance.ICDevice;
import com.example.smartdrive.instance.Phase;
import com.example.smartdrive.ui.database.DatabaseViewModel;


public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    //test
    boolean isPaused = true;
    private double maxSpeed = 0;

    private View root;
    private TextView txtGPS, txtRoadName, txtDirection, txtSpeed, txtRoadList;
    private Button btnGetGPS;

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
        homeViewModel =
                ViewModelProviders.of(getActivity()).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);

        mHandler = new Handler();
        greenIndex = new int[PANEL_COUNT][];

        bindView();
        setViewModel();
        setListener();

        startRepeatingTask();

        return root;
    }

    private void bindView() {
        txtGPS = root.findViewById(R.id.txtGPS);
        txtRoadName = root.findViewById(R.id.txtRoadName);
        txtDirection = root.findViewById(R.id.txtDirection);
        txtSpeed = root.findViewById(R.id.txtSpeed);
        txtRoadList = root.findViewById(R.id.txtRoadList);
        btnGetGPS = root.findViewById(R.id.btnGetGPS);

        layout1 = root.findViewById(R.id.layout1);
        layout2 = root.findViewById(R.id.layout2);
        layout1.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);

        txtCountdown = new TextView[PANEL_COUNT];
        txtCountdown[0] = root.findViewById(R.id.txtCountdown1);
        txtCountdown[1] = root.findViewById(R.id.txtCountdown2);

        imageView = new ImageView[PANEL_COUNT][4];
        imageView[0][0] = root.findViewById(R.id.imgLight0);
        imageView[0][1] = root.findViewById(R.id.imgLight1);
        imageView[0][2] = root.findViewById(R.id.imgLight2);
        imageView[0][3] = root.findViewById(R.id.imgLight3);
        imageView[1][0] = root.findViewById(R.id.imgLight4);
        imageView[1][1] = root.findViewById(R.id.imgLight5);
        imageView[1][2] = root.findViewById(R.id.imgLight6);
        imageView[1][3] = root.findViewById(R.id.imgLight7);

        setLightGone(0);
        setLightGone(1);
    }

    private void setViewModel() {
        homeViewModel.getDirection().observe(this, new Observer<Direction>() {
            @Override
            public void onChanged(@Nullable Direction d) {
                //if (d.dLng != 0 && d.dLat != 0)
                txtDirection.setText(d.dLng + ", " + d.dLat);
            }
        });
        homeViewModel.getCurLocation().observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                double curLng = location.getLongitude();
                double curLat = location.getLatitude();
                String msg = String.format("%f, %f", curLng, curLat);
                txtGPS.setText(msg);
                //Log.e("onChanged", msg);

                double speed = location.getSpeed() * 3.6;
                //Log.e("Speed", String.valueOf(speed));
                if (speed > maxSpeed) maxSpeed = speed;
                txtSpeed.setText("目前速度：" + String.valueOf(speed) + " km/h"/* + "\n最高速度：" + String.valueOf(maxSpeed)*/);
            }
        });
        homeViewModel.getRoadName().observe(this, new Observer<String>() {
            @Override
        public void onChanged(String s) {
            txtRoadName.setText(s);
        }
    });
        homeViewModel.getRoadsStr().observe(this, new Observer<String>() {
        @Override
        public void onChanged(String s) {
            //txtRoadList.setText(s);
        }
    });

        homeViewModel.getDevice().observe(this, new Observer<ICDevice>() {
            @Override
            public void onChanged(ICDevice icDevice) {
                if (isPaused) {
                    txtRoadList.setText(icDevice.deviceID);
                } else {

                }
            }
        });

        homeViewModel.getStatusStr().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                //txtCountdown1.setText(s);
            }
        });

        homeViewModel.getPhases().observe(this, new Observer<Phase[]>() {
            @Override
            public void onChanged(Phase[] phases) {
                layout1.setVisibility(View.VISIBLE);
                for (int i = 0; i < PANEL_COUNT; i++) {
                    greenIndex[i] = new int[]{2};
                    setLightGone(i);
                }
                Log.d("PhaseState", phases[0].phaseState);
                Log.d("PhaseState", "countdown: "+phases[0].countdown);
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

        btnGetGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxSpeed = 0;
                isPaused = !isPaused;
                if (isPaused)
                    txtRoadList.setText("");
                else
                    txtRoadList.setText("暫停");
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