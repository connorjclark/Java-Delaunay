package com.hoten.delaunay.examples;

import com.hoten.delaunay.voronoi.VoronoiGraph;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h3>How to use:</h3>
 * Just change constants to customize graph, it's shape and image.
 */
public class TestDriver {

    /** Do you really need to save image? */
    private static final boolean SAVE_FILE = false;

    /** The side of the square in which the graph will be fitted. */
    private static final int GRAPH_BOUNDS = 1000;

    /** Size of image which will be drawn. */
    private static final int FRAME_BOUNDS = 512;

    /** Number of pieces for the graph. */
    private static final int SITES_AMOUNT = 30_000;

    /**
     * Each time a relaxation step is performed, the points are left in a slightly more even distribution:
     * closely spaced points move farther apart, and widely spaced points move closer together.
     */
    private static final int LLOYD_RELAXATIONS = 2;

    /** Randomizing number. Use it with {@link #RANDOM_SEED} = false to get same image every time. */
    private static long SEED = 123L;

    /** You can make it false if you want to check some changes in code or image/graph size. */
    private static final boolean RANDOM_SEED = true;

    public static void main(String[] args) throws IOException {
        if (RANDOM_SEED) SEED = System.nanoTime();

        printInfo();

        final BufferedImage img = createVoronoiGraph(GRAPH_BOUNDS, SITES_AMOUNT, LLOYD_RELAXATIONS, SEED).createMap();

        saveFile(img);

        showGraph(img);
    }

    private static void printInfo() {
        System.out.println("Seed: " + SEED);
        System.out.println("Bounds: " + GRAPH_BOUNDS);
        System.out.println("Sites: " + SITES_AMOUNT);
        System.out.println("Relaxs: " + LLOYD_RELAXATIONS);
        System.out.println("=============================");
    }

    public static VoronoiGraph createVoronoiGraph(int bounds, int numSites, int numLloydRelaxations, long seed) {
        final Random r = new Random(seed);

        //make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(numSites, bounds, bounds, r, null);

        //assemble the voronoi strucutre into a usable graph object representing a map
        final TestGraphImpl graph = new TestGraphImpl(v, numLloydRelaxations, r);

        return graph;
    }

    private static void saveFile(BufferedImage img) throws IOException {
        if (SAVE_FILE) {
            File file = new File(String.format("output/seed-%s-sites-%d-lloyds-%d.png", SEED, SITES_AMOUNT, LLOYD_RELAXATIONS));
            file.mkdirs();
            while (file.exists()) file = new File(incrementFileName(file.getPath()));
            ImageIO.write(img, "PNG", file);
        }
    }

    /**
     * If you have equal filenames - use this method to change filename before creating it.
     *
     * @param oldName fileName_index1.format(fileName.format)
     * @return fileName_index2.format(fileName_1.format)
     */
    private static String incrementFileName(String oldName) {
        String newName;
        int i = oldName.lastIndexOf('.');
        Matcher m = Pattern.compile("\\((\\d+)\\).").matcher(oldName);
        if (m.find()) {
            String n = String.valueOf(Integer.valueOf(m.group(1)) + 1);
            newName = oldName.substring(0, m.start()) + "(" + n + ")" + oldName.substring(i);
        } else {
            newName = oldName.substring(0, i) + "(1)" + oldName.substring(i);
        }
        return newName;
    }

    private static int oldX = -1, oldY = -1;
    private static int drawX = 0, drawY = 0;

    private static void showGraph(final BufferedImage img) {
        final JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(img, getInsets().left + drawX, getInsets().top - drawY, null);
            }
        };

        frame.setTitle("Java Fortune");
        frame.setVisible(true);
        frame.setSize(FRAME_BOUNDS, FRAME_BOUNDS);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                oldX = e.getX();
                oldY = e.getY();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                oldX = -1;
                oldY = -1;
            }
        });
        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (oldX != -1) {
                    int dx = e.getX() - oldX, dy = e.getY() - oldY;
                    drawX = Math.min(0, Math.max(FRAME_BOUNDS - GRAPH_BOUNDS, drawX + dx));
                    drawY = Math.min(GRAPH_BOUNDS - FRAME_BOUNDS, Math.max(0, drawY - dy));
                    oldX += dx;
                    oldY += dy;
                    frame.repaint();
                }
            }
        });
    }
}
