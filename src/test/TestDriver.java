package test;

import hoten.geom.Point;
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
 * TestDriver.java
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

        //make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(numSites, width, height, r, null);

        //assemble the voronoi strucutre into a usable graph object representing a map
        final TestGraphImpl graph = new TestGraphImpl(v, 2, r);

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
