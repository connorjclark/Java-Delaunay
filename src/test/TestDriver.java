/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import hoten.geom.Point;
import hoten.geom.Rectangle;
import hoten.voronoi.VoronoiGraph;
import hoten.voronoi.nodename.as3delaunay.Voronoi;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * TestDriver.java Function Date Jun 14, 2013
 *
 * @author Connor
 */
public class TestDriver {

    public static void main(String[] args) {
        final int width = 600;
        final int height = 600;
        final int numSites = 3000;
        final ArrayList<Point> points = new ArrayList();
        final long seed = System.nanoTime();
        final Random r = new Random(seed);
        System.out.println("seed: " + seed);

        //let's create a bunch of random points
        for (int i = 0; i < numSites; i++) {
            points.add(new Point(r.nextDouble() * width, r.nextDouble() * height));
        }

        //now make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(points, null, new Rectangle(0, 0, width, height));

        //assemble the voronoi strucutre into a usable graph object representing a map
        final VoronoiGraph graph = new VoronoiGraph(v, 2, r);

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        graph.paint(g);

        final JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(img, 25, 35, null);
            }
        };
        frame.setTitle("java fortune");
        frame.setVisible(true);
        frame.setSize(width + 50, height + 50);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
