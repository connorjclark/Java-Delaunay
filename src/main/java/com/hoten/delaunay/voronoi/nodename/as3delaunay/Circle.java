package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.Point;

public final class Circle extends Object {

    public Point center;
    public double radius;

    public Circle(double centerX, double centerY, double radius) {
        super();
        this.center = new Point(centerX, centerY);
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "Circle (center: " + center + "; radius: " + radius + ")";
    }
}