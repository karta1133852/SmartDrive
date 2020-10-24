package com.example.smartdrive.instance;

import org.json.JSONObject;

import java.util.ArrayList;

public class Node {

    public double lng, lat;
    public String nodeID;
    public ArrayList<String> roads;
    public JSONObject nodeData;

    public Node(String nodeID) {
        this.nodeID = nodeID;
    }

    public Node(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public Node(double lng, double lat, String nodeID) {
        this.lng = lng;
        this.lat = lat;
        this.nodeID = nodeID;
    }

    public Node(double lng, double lat, ArrayList<String> roads, String nodeID) {
        this.lng = lng;
        this.lat = lat;
        this.roads = roads;
        this.nodeID = nodeID;
    }

    public double checkOnLine(Node a, Node b) {

        return 0.0d;
    }

    public double distToSegment(Node v, Node w) { return Math.sqrt(distToSegmentSquared(v, w)); }

    private double sqr(double x) { return Math.pow(x, 2); }
    private double dist2(Node v, Node  w) { return sqr(v.lng - w.lng) + sqr(v.lat - w.lat); }
    private double distToSegmentSquared(Node v, Node w) {
        double l2 = dist2(v, w);
        if (l2 == 0) return dist2(this, v);
        double t = ((this.lng - v.lng) * (w.lng - v.lng) + (this.lat - v.lat) * (w.lat - v.lat)) / l2;
        t = Math.max(0, Math.min(1, t));
        return dist2(this, new Node(v.lng + t * (w.lng - v.lng),
                v.lat + t * (w.lat - v.lat)));
    }
}
