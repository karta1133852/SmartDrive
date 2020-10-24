package com.example.smartdrive.util;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartdrive.MainActivity;
import com.example.smartdrive.callback.ArrayListCallback;
import com.example.smartdrive.callback.DeviceCallback;
import com.example.smartdrive.callback.NodeCallback;
import com.example.smartdrive.callback.StringCallback;
import com.example.smartdrive.instance.Direction;
import com.example.smartdrive.instance.ICDevice;
import com.example.smartdrive.instance.Node;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;


public class Traffic {

    // km/hr
    private final static double MIN_DETECT_SPEED = 1;

    private static DatabaseReference refRoot = FirebaseDatabase.getInstance().getReference();

    public static void getDeviceList(final ArrayListCallback arrayListCallback) {
        DatabaseReference refDevice = refRoot.child("DeviceInfo").child("Device");
        refDevice.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> nameList = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //String currentName = ds.child("ICName").getValue().toString();
                    nameList.add(ds.getKey());
                }
                arrayListCallback.callback(nameList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void getDeviceInfo(final String deviceID, final DeviceCallback deviceCallback) {

        final ICDevice device = new ICDevice();
        device.deviceID = deviceID;

        DatabaseReference refTraffic = refRoot.child("DeviceInfo").child("Traffic").child(deviceID);
        refTraffic.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Gson gson = new Gson();
                String str = gson.toJson(dataSnapshot.getValue());
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    device.phaseData = jsonObject;
                    ICDevice updatedDevice = updateDevicePhase(device);
                    deviceCallback.callback(updatedDevice);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override

            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static ICDevice updateDevicePhase(ICDevice device) {

        if (Calendar.getInstance().getTimeInMillis() < device.nextPlanMillisecond)
            return device;

        JSONObject phases = device.phaseData;

        Calendar calendar = Calendar.getInstance();
        int weekday = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int time = hour * 100 + minute;
        try {
            JSONArray arrTimes = phases.getJSONArray("Times");
            JSONArray arr = arrTimes.getJSONArray(weekday);
            String planID;
            for (int i = arr.length()-1; i >= 0 ; i--) {
                int planTime = arr.getJSONObject(i).getInt("Time");
                if (planTime <= time) {
                    planID = arr.getJSONObject(i).getString("PlanID");
                    Calendar currentPlanTime = Calendar.getInstance();
                    currentPlanTime.set(Calendar.HOUR_OF_DAY, planTime/100);
                    currentPlanTime.set(Calendar.MINUTE, planTime%100);
                    currentPlanTime.set(Calendar.SECOND, 0);
                    currentPlanTime.set(Calendar.MILLISECOND, 0);
                    device.currentPlanMillisecond = currentPlanTime.getTimeInMillis();

                    JSONObject plan = phases.getJSONObject("Plans").getJSONObject(planID);
                    device.phaseOrder = plan.getString("PhaseOrder");

                    int len = plan.length()-1;
                    device.green = new int[len];
                    device.yellow = new int[len];
                    device.allred = new int[len];
                    device.cycle = new int[len];

                    for (int j = 0; j < len; j++) {
                        device.green[j] = plan.getJSONObject(String.valueOf(j+1)).getInt("Green");
                        device.yellow[j] = plan.getJSONObject(String.valueOf(j+1)).getInt("Yellow");
                        device.allred[j] = plan.getJSONObject(String.valueOf(j+1)).getInt("AllRed");
                        device.cycle[j] = plan.getJSONObject(String.valueOf(j+1)).getInt("Cycle");
                        Log.d("phase", device.green[j]+","+device.yellow[j]+","+device.allred[j]+","+device.cycle[j]);
                    }

                    if (i == arr.length()-1) {
                        currentPlanTime.add(Calendar.DAY_OF_MONTH, +1);
                        currentPlanTime.set(Calendar.HOUR_OF_DAY, 0);
                        currentPlanTime.set(Calendar.MINUTE, 0);
                    } else {
                        planTime = arr.getJSONObject(i+1).getInt("Time");
                        currentPlanTime.set(Calendar.HOUR_OF_DAY, planTime/100);
                        currentPlanTime.set(Calendar.MINUTE, planTime%100);
                    }
                    device.nextPlanMillisecond = currentPlanTime.getTimeInMillis();
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return device;
    }

    public static void getNearbyNodes(final double lng, final double lat, double range, final StringCallback stringCallback) {
        final double EARTH_RADIUS = 6371 * 1000;
        final Node node = new Node(lng, lat);
        final double dlng = Math.abs(Math.toDegrees(2 * Math.asin(Math.sin(range / (2 * EARTH_RADIUS) / Math.cos(Math.toDegrees(lat))))));
        //dlng = Math.abs(Math.toDegrees(dlng));
        final double dlat = Math.abs(Math.toDegrees(range / EARTH_RADIUS));

        final double[] min = { Double.MAX_VALUE };

        DatabaseReference refNodes = refRoot.child("Nodes");
        Log.e("Test", dlat+"");
        //Query queryByLng = refNodes.orderByChild("lng").startAt(lng-dlng).endAt(lng+dlng);
        refNodes.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("test", "!!!!!!!   ");
                int count = 0;
                ArrayList<Node> nodes = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    double _lng = Double.parseDouble(ds.child("lng").getValue().toString());
                    double _lat = Double.parseDouble(ds.child("lat").getValue().toString());
                    if (_lat > lat-dlat && _lat < lat+dlat && _lng > lng-dlng && _lng < lng+dlng) {
                        count++;
                        GenericTypeIndicator<ArrayList<String>> gti = new GenericTypeIndicator<ArrayList<String>>() {};
                        ArrayList<String> roads = ds.child("Road").getValue(gti);
                        nodes.add(new Node(_lng, _lat, roads, ds.getKey().toString()));
                        //Log.e("Test", _lng + ", " + _lat);
                    }
                }
                Log.e("Test", count+"");

                Node a = node, b = node;
                double min = Double.MAX_VALUE;
                for (int i = 0; i < count-1; i++) {
                    for (int j = i+1; j < count; j++) {
                        double dist = node.distToSegment(nodes.get(i), nodes.get(j));
                        /*a = nodes.get(i);
                        b = nodes.get(j);
                        Log.e("Test", a.roads.get(0) + " || " + b.roads.get(0) + " : " + dist);*/
                        //Log.e("Test", i + ", " + j);
                        if (dist < min) {
                            min = dist;
                            a = nodes.get(i);
                            b = nodes.get(j);
                        }
                    }
                }
                Log.e("Test", "min: " + min);
                String roadName = getRoadName(a.roads, b.roads);
                stringCallback.callback(roadName);
                Log.e("Test", roadName);
                Log.e("Test", a.nodeID);
                Log.e("Test", b.nodeID);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //Query queryByLat = refNodes.orderByChild("lat").startAt(lat-dlat).endAt(lat+dlat);
    }

    public static ArrayList<String> getDeviceList(JSONObject jsonObject) {
        ArrayList<String> deivceList = new ArrayList<>();
        try {
            JSONArray roadJSONArray = jsonObject.getJSONArray("DeviceList");;
            for (int i = 0; i < roadJSONArray.length(); i++){
                deivceList.add(roadJSONArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return deivceList;
    }

    public static void getNearbyNodes(JSONObject jsonObject, Location location, final Direction direction, double range,
                                      final StringCallback stringCallback, final DeviceCallback deviceCallback) {
        double lng = location.getLongitude();
        double lat = location.getLatitude();
        double EARTH_RADIUS = 6371 * 1000;
        Node currentNode = new Node(lng, lat);
        double dlng = Math.abs(Math.toDegrees(2 * Math.asin(Math.sin(range / (2 * EARTH_RADIUS) / Math.cos(Math.toDegrees(lat))))));
        double dlat = Math.abs(Math.toDegrees(range / EARTH_RADIUS));

        Node node1 = null, node2 = null;
        double min = Double.MAX_VALUE;
        Iterator iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            try {
                String nodeID = iterator.next().toString();
                JSONObject jsonNode = jsonObject.getJSONObject(nodeID);
                double _lng = jsonNode.getDouble("lng");
                double _lat = jsonNode.getDouble("lat");
                Node nodeFound = getNodeByID(jsonObject, nodeID);
                if (_lat > lat-dlat && _lat < lat+dlat && _lng > lng-dlng && _lng < lng+dlng) {
                    JSONArray connectedJSONArray = jsonNode.getJSONArray("ConnectedNodes");
                    for (int i = 0; i < connectedJSONArray.length(); i++){
                        String connectedID = connectedJSONArray.getString(i);
                        if (connectedID.equals(""))
                            continue;
                        Node nodeConnected = getNodeByID(jsonObject, connectedID);
                        double dist = currentNode.distToSegment(nodeFound, nodeConnected);
                        if (dist < min) {
                            min = dist;
                            node1 = nodeFound; node2 = nodeConnected;
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        //Log.e("Test", "min: " + min);
        if (node1 != null && node2 != null) {
            boolean hasSpeed = false;
            if (location.getSpeed() * 3.6 > MIN_DETECT_SPEED) {
                if (checkSwtichApproachedNode(node1, node2, direction)) {
                    Node tmp = node1;
                    node1 = node2;
                    node2 = tmp;
                }
                hasSpeed = true;
            }

            /**/
            //Node tmp = node1; node1 = node2; node2 = tmp;
            /**/

            final String _node2ID = node2.nodeID;
            final DatabaseReference refNodes = refRoot.child("Nodes");
            final boolean finalHasSpeed = hasSpeed;
            getNodeByID(node1.nodeID, new NodeCallback() {
                @Override
                public void callback(final Node _node1) {
                    getNodeByID(_node2ID, new NodeCallback() {
                        @Override
                        public void callback(Node _node2) {
                            String roadName = getRoadName(_node1.roads, _node2.roads);
                            stringCallback.callback(roadName);
                            Log.e("Test", roadName);

                            if (finalHasSpeed)
                                getDeviceByNode(_node1, _node2, direction, deviceCallback);
                        }
                    });
                }
            });

            Log.e("Test", node1.nodeID);
            Log.e("Test", node2.nodeID);
        } else {
            stringCallback.callback("找不到路名");
            Log.e("Test", "找不到路名");
        }
    }

    public static void getRoads(String nodeID, final ArrayListCallback arrayListCallback) {
        final DatabaseReference refNodes = refRoot.child("Nodes");
        refNodes.child(nodeID).child("Roads").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> roads = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    roads.add(ds.getValue().toString());
                }
                arrayListCallback.callback(roads);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private static boolean checkSwtichApproachedNode(Node node1, Node node2, Direction direction) {
        Double dLng = node1.lng - node2.lng;
        Double dLat = node1.lat - node2.lat;
        double angle1 = angleBetweenVectors(new Direction(dLng, dLat), direction);
        double angle2 = angleBetweenVectors(new Direction(-dLng, -dLat), direction);
        Log.e("angleCompare", angle1 + ", " + angle2);
        if (angle1 > angle2)
            return true;
        return false;
    }

    private static Node getNodeByID(JSONObject jsonObject, String nodeID) {
        Node node = null;
        try {
            JSONObject jsonNode = jsonObject.getJSONObject(nodeID);
            double _lng = jsonNode.getDouble("lng");
            double _lat = jsonNode.getDouble("lat");
            /*ArrayList<String> roads = new ArrayList<>();
            JSONArray roadJSONArray = jsonNode.getJSONArray("Road");;
            for (int i = 0; i < roadJSONArray.length(); i++){
                roads.add(roadJSONArray.getString(i));
            }*/
            node = new Node(_lng, _lat, nodeID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return node;
    }

    private static void getNodeByID(final String nodeID, final NodeCallback nodeCallback) {
        refRoot.child("Nodes").child(nodeID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Node node = new Node(nodeID);
                final ArrayList<String> roads = new ArrayList<>();
                try {
                    node.nodeData = new JSONObject(new Gson().toJson(dataSnapshot.getValue()));
                    node.lng = node.nodeData.getDouble("lng");
                    node.lat = node.nodeData.getDouble("lat");
                    JSONArray arrRoads1 = node.nodeData.getJSONArray("Roads");
                    for (int i = 0; i < arrRoads1.length(); i++) {
                        roads.add(arrRoads1.getString(i));
                    }
                    node.roads = roads;

                    nodeCallback.callback(node);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void getDeviceByNode(final Node node1, final Node node2, final Direction direction, final DeviceCallback deviceCallback) {
        try {
            final JSONObject nodeData1 = node1.nodeData;
            final JSONObject nodeData2 = node2.nodeData;
            boolean isNode1Intersection = nodeData1.getBoolean("Intersection");
            if (isNode1Intersection) {
                final String deviceID = nodeData1.getString("DeviceID");
                String otherICNodeID;
                boolean isNode2Intersection = nodeData2.getBoolean("Intersection");
                if (isNode2Intersection) {
                    otherICNodeID = node2.nodeID;
                } else {
                    JSONArray nextIC = node2.nodeData.getJSONArray("NextIC");
                    int indexIC = (nextIC.getString(0).equals(node1.nodeID)) ? 1 : 0;
                    otherICNodeID = nextIC.getString(indexIC);
                }
                final String _otherICNodeID = otherICNodeID;
                getDeviceInfo(deviceID, new DeviceCallback() {
                    @Override
                    public void callback(ICDevice device) {
                        try {
                            Log.d("TestPhase", "node1: " + node1.nodeID);
                            Log.d("TestPhase", "node2: " + node2.nodeID);
                            Log.d("TestPhase", "otherICNodeID: " + _otherICNodeID);
                            device.planOrder = nodeData1.getJSONObject("Phase").getInt(_otherICNodeID);
                            Log.d("TestPhase", "planOrder: " + device.planOrder);
                        } catch (JSONException e) {
                            device.planOrder = -1;
                            e.printStackTrace();
                        }
                        deviceCallback.callback(device);
                    }
                });
            } else {
                int indexIC;
                boolean checkEast = nodeData1.getBoolean("CheckEast");
                if (checkEast)
                    indexIC = (direction.dLng > 0) ? 0 : 1;
                else
                    indexIC = (direction.dLat > 0) ? 0 : 1;
                final String nextICNodeID = nodeData1.getJSONArray("NextIC").getString(indexIC);
                final String otherICNodeID = nodeData1.getJSONArray("NextIC").getString((indexIC+1)%2);
                if (!nextICNodeID.equals("")) {
                    final JSONObject nextICNodeObject = MainActivity.nodes.getJSONObject(nextICNodeID);
                    String deviceID = nextICNodeObject.getString("DeviceID");

                    getDeviceInfo(deviceID, new DeviceCallback() {
                        @Override
                        public void callback(ICDevice device) {
                            try {
                                Log.d("TestPhase", "node1: " + node1.nodeID);
                                Log.d("TestPhase", "node2: " + node2.nodeID);
                                Log.d("TestPhase", "otherICNodeID: " + otherICNodeID);
                                device.planOrder = nextICNodeObject.getJSONObject("Phase").getInt(otherICNodeID);
                                Log.d("TestPhase", "planOrder: " + device.planOrder);
                            } catch (JSONException e) {
                                device.planOrder = -1;
                                e.printStackTrace();
                            }
                            deviceCallback.callback(device);
                        }
                    });
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // radius
    private static double angleBetweenVectors(Direction v1, Direction v2) {
        double x1 = v1.dLng, y1 = v1.dLat;
        double x2 = v2.dLng, y2 = v2.dLat;
        double det = x1*y2 - y1*x2;
        double dot = x1*x2 + y1*y2;
        double angle = Math.abs(Math.atan2(det, dot));
        return angle;
    }

    private static String getRoadName(ArrayList<String> a, ArrayList<String> b) {
        for (String road1: a) {
            for (String road2: b) {
                if (road1.equals(road2))
                    return road1;
            }
        }
        return "";
    }
}
