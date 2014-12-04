package hoten.delaunay;

import hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class TestDriver {

    public static void main(String[] args) throws IOException {
        int bounds = 2000;
        int numSites = 10000;
        long seed = System.nanoTime();
        System.out.println("seed: " + seed);

        final BufferedImage img = createMap(bounds, numSites, seed);

        ImageIO.write(img, "PNG", new File(String.format("%s.png", seed)));

        final JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(img, 25, 35, null);
            }
        };

        frame.setTitle("java fortune");
        frame.setVisible(true);
        frame.setSize(img.getWidth() + 50, img.getHeight() + 50);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static BufferedImage createMap(int bounds, int numSites, long seed) {
        final Random r = new Random(seed);

        //make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(numSites, bounds, bounds, r, null);

        //assemble the voronoi strucutre into a usable graph object representing a map
        final TestGraphImpl graph = new TestGraphImpl(v, 2, r);

        final BufferedImage img = new BufferedImage(bounds, bounds, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bounds, bounds);

        graph.paint(g);

        return img;
    }
}
