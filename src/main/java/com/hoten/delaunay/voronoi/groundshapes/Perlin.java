package com.hoten.delaunay.voronoi.groundshapes;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.Random;

public class Perlin implements HeightAlgorithm {
    @Override
    public boolean isWater(Point p, Rectangle bounds, Random random) {
        throw new UnsupportedOperationException("Algorithm doesn't have implementation. " +
                "Please use another implementation or make your own.");
        /*if (noise == null) {
         noise = new Perlin2d(.125, 8, MyRandom.seed).createArray(257, 257);
         }
         int x = (int) ((p.x + 1) * 128);
         int y = (int) ((p.y + 1) * 128);
         return noise[x][y] < .3 + .3 * p.l2();*/

        /*boolean eye1 = new Point(p.x - 0.2, p.y / 2 + 0.2).length() < 0.05;
         boolean eye2 = new Point(p.x + 0.2, p.y / 2 + 0.2).length() < 0.05;
         boolean body = p.length() < 0.8 - 0.18 * Math.sin(5 * Math.atan2(p.y, p.x));
         return !(body && !eye1 && !eye2);*/
    }
}