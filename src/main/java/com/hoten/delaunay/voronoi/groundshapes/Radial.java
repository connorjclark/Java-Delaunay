package com.hoten.delaunay.voronoi.groundshapes;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.Random;

/**
 *
 */
public class Radial implements HeightAlgorithm {

    private final double ISLAND_FACTOR;  // 1.0 means no small islands; 2.0 leads to a lot
    private final int bumps;
    private final double startAngle;
    private final double dipAngle;
    private final double dipWidth;

    /**
     *
     * @param ISLAND_FACTOR 1.0 means no small islands, 2.0 leads to a lot
     * @param bumps
     * @param startAngle
     * @param dipAngle
     * @param dipWidth
     */
    public Radial(double ISLAND_FACTOR, int bumps, double startAngle, double dipAngle, double dipWidth) {
        this.ISLAND_FACTOR = ISLAND_FACTOR;
        this.bumps = bumps;
        this.startAngle = startAngle;
        this.dipAngle = dipAngle;
        this.dipWidth = dipWidth;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWater(Point p, Rectangle bounds, Random r) {
        p = new Point(2 * (p.x / bounds.width - 0.5), 2 * (p.y / bounds.height - 0.5));

        double angle = Math.atan2(p.y, p.x);
        double length = 0.5 * (Math.max(Math.abs(p.x), Math.abs(p.y)) + p.length());

        double r1 = 0.5 + 0.40 * Math.sin(startAngle + bumps * angle + Math.cos((bumps + 3) * angle));
        double r2 = 0.7 - 0.20 * Math.sin(startAngle + bumps * angle - Math.sin((bumps + 2) * angle));
        if (Math.abs(angle - dipAngle) < dipWidth
                || Math.abs(angle - dipAngle + 2 * Math.PI) < dipWidth
                || Math.abs(angle - dipAngle - 2 * Math.PI) < dipWidth) {
            r1 = r2 = 0.2;
        }
        return !(length < r1 || (length > r1 * ISLAND_FACTOR && length < r2));
    }
}