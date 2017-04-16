package com.hoten.delaunay.voronoi.groundshapes;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.Random;

/**
 * Code from article
 * <a href="http://devmag.org.za/2009/04/25/perlin-noise/">How to Use Perlin Noise in Your, GamesHerman Tulleken</a>.
 *
 * This algorithm do next steps:
 * <ol start = "1">
 *     <li>Create white noise as base for perlin noise.</li>
 *     <li>Smooth it {@code octaveCount} times.</li>
 * </ol>
 */
public class Perlin implements HeightAlgorithm {

    private final Random r;
    private final float median;
    private final float noise[][];

    /**
     * @param random Randomizer.
     * @param octaveCount Smooth value. 0 means there will be only white noise.
     * @param noiseWidth Width of noise map. Should be less than graph width.
     * @param noiseHeight Height of noise map. Should be less than graph width.
     */
    public Perlin(Random random, int octaveCount, int noiseWidth, int noiseHeight) {
        this.r = random;

        float whiteNoise[][] = generateWhiteNoise(noiseWidth + 1, noiseHeight + 1);

        noise = generatePerlinNoise(whiteNoise, octaveCount);

        median = findMedian();
    }

    /**
     * @return The value dividing ground and water.
     */
    private float findMedian() {
        long count[] = new long[10];

        for (float[] aNoise : noise) {
            for (float aaNoise : aNoise) {
                for (int k = 1; k < count.length; k++) {
                    if (aaNoise * 10 < k) {
                        count[k - 1]++;
                        break;
                    }
                }
            }
        }

        int n = 0;

        for (int i = 1; i < count.length; i++) {
            if (count[i] > count[n]) n = i;
        }

        return (float) (n + 0.5) / 10;
    }

    /**
     *
     * @param random Randomizer.
     * @param octaveCount Smooth value. 0 means there will be only white noise.
     */
    public Perlin(Random random, float centrical, int octaveCount) {
        this(random, octaveCount, 256, 256);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWater(Point p, Rectangle bounds, Random random) {
        int x = (int) (p.x / bounds.width * (noise.length - 1));

        int y = (int) (p.y / bounds.height * (noise[0].length - 1));

        return noise[x][y] < median;
    }

    /**
     *
     * @param width Width of noise map.
     * @param height height of noise map.
     * @return White noise map.
     */
    private float[][] generateWhiteNoise(int width, int height) {
        float[][] noise = new float[width][height];

        for (int i = width / 25; i < width * 0.96; i++) {
            for (int j = height / 25; j < height * 0.96; j++) {
                noise[i][j] = r.nextFloat();
            }
        }

        return noise;
    }

    /**
     *
     * @param baseNoise Noise map which will be processed.
     * @param octaveCount Smooth value. 0 means there will be only white noise.
     * @return Perlin noise.
     */
    private float[][] generatePerlinNoise(float[][] baseNoise, int octaveCount) {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][][] smoothNoise = new float[octaveCount][][]; //an array of 2D arrays containing

        float persistance = 0.5f;

        //generate smooth noise
        for (int i = 0; i < octaveCount; i++) {
            smoothNoise[i] = generateSmoothNoise(baseNoise, i);
        }

        float[][] perlinNoise = new float[width][height];
        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;

        //blend noise together
        for (int octave = octaveCount - 1; octave >= 0; octave--) {
            amplitude *= persistance;
            totalAmplitude += amplitude;

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    perlinNoise[i][j] += smoothNoise[octave][i][j] * amplitude;
                }
            }
        }

        //normalisation
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] /= totalAmplitude;
            }
        }

        return perlinNoise;
    }

    /**
     *
     * @param baseNoise Noise map which will be used to create a smoother noise map.
     * @param octave Smooth value. 0 means there will be only white noise.
     * @return Smoother noise map than a base map.
     */
    private float[][] generateSmoothNoise(float[][] baseNoise, int octave) {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][] smoothNoise = new float[width][height];

        int samplePeriod = 1 << octave; // calculates 2 ^ k
        float sampleFrequency = 1.0f / samplePeriod;

        for (int i = 0; i < width; i++) {
            //calculate the horizontal sampling indices
            int sample_i0 = (i / samplePeriod) * samplePeriod;
            int sample_i1 = (sample_i0 + samplePeriod) % width; //wrap around
            float horizontal_blend = (i - sample_i0) * sampleFrequency;

            for (int j = 0; j < height; j++) {
                //calculate the vertical sampling indices
                int sample_j0 = (j / samplePeriod) * samplePeriod;
                int sample_j1 = (sample_j0 + samplePeriod) % height; //wrap around
                float vertical_blend = (j - sample_j0) * sampleFrequency;

                //blend the top two corners
                float top = interpolate(baseNoise[sample_i0][sample_j0],
                        baseNoise[sample_i1][sample_j0], horizontal_blend);

                //blend the bottom two corners
                float bottom = interpolate(baseNoise[sample_i0][sample_j1],
                        baseNoise[sample_i1][sample_j1], horizontal_blend);

                //final blend
                smoothNoise[i][j] = interpolate(top, bottom, vertical_blend);
            }
        }

        return smoothNoise;
    }

    /**
     * Function returns a linear interpolation between two values.
     * Essentially, the closer alpha is to 0, the closer the resulting value will be to x0;
     * the closer alpha is to 1, the closer the resulting value will be to x1.
     *
     * @param x0 First value.
     * @param x1 Second value.
     * @param alpha Transparency.
     * @return Linear interpolation between two values.
     */
    private static float interpolate(float x0, float x1, float alpha) {
        return x0 * (1 - alpha) + alpha * x1;
    }
}