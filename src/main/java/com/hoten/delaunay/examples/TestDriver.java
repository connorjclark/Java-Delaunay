package com.hoten.delaunay.examples;

import com.hoten.delaunay.voronoi.VoronoiGraph;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * <h3>How to use:</h3>
 * Just change constants to customize graph, it's shape and image.
 */
public class TestDriver {

    /** Do you really need to save image? */
    private static final boolean SAVE_FILE = false;

    /** Image size (and frame size too for this example). */
    private static final int BOUNDS = 1000;

    /** Number of pieces for the graph. */
    private static final int SITES_AMOUNT = 30_000;

    /** Each time a relaxation step is performed, the points are left in a slightly more even distribution:
     * closely spaced points move farther apart, and widely spaced points move closer together. */
    private static final int LLOYD_RELAXATIONS = 2;

    /** Randomizing number. Use it with {@link #RANDOM_SEED} = false to get same image every time. */
    private static long SEED = 123L;

    /** You can make it false if you want to check some changes in code or image/graph size. */
    private static final boolean RANDOM_SEED = true;

    public static void main(String[] args) throws IOException {
        if (RANDOM_SEED) SEED = System.nanoTime();

        printInfo();

        final BufferedImage img = createVoronoiGraph(BOUNDS, SITES_AMOUNT, LLOYD_RELAXATIONS, SEED).createMap();

        saveFile(img);

        showGraph(img);
    }

    private static void printInfo() {
        System.out.println("Seed: " + SEED);
        System.out.println("Bounds: " + BOUNDS);
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

    private static void showGraph(final BufferedImage img) {
        final JFrame frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(img, getInsets().left - 1, getInsets().top - 1, null);
            }
        };

        frame.setTitle("Java fortune");
        frame.setVisible(true);
        frame.setSize(img.getWidth() + frame.getInsets().left + frame.getInsets().right - 2,
                img.getHeight() + frame.getInsets().top + frame.getInsets().bottom - 2);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
