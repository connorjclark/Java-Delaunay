package com.hoten.delaunay.geom;

/**
 * Point.java
 *
 * @author Connor
 */
public class Point {

    public static double distance(Point _coord, Point _coord0) {
        return Math.sqrt((_coord.x - _coord0.x) * (_coord.x - _coord0.x) + (_coord.y - _coord0.y) * (_coord.y - _coord0.y));
    }
    public double x, y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return x + ", " + y;
    }

    public double l2() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }
}
