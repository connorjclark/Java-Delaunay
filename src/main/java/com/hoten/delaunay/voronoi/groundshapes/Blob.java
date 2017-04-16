package com.hoten.delaunay.voronoi.groundshapes;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.Random;

/**
 * A blob with eyes.
 */
public class Blob implements HeightAlgorithm {

    /** {@inheritDoc} */
    @Override
    public boolean isWater(Point point, Rectangle bounds, Random random) {
        Point p = new Point(2 * (point.x / bounds.width - 0.5), 2 * (point.y / bounds.height - 0.5));
        boolean eye1 = new Point(p.x - 0.2, p.y / 2 + 0.2).length() < 0.05;
        boolean eye2 = new Point(p.x + 0.2, p.y / 2 + 0.2).length() < 0.05;
        boolean body = p.length() < 0.8 - 0.18 * Math.sin(5 * Math.atan2(p.y, p.x));
        return !body || eye1 || eye2;
    }
}