package hoten.utils;

/**
 * MyRandom.java Function Date Jun 11, 2013
 *
 * @author Connor
 */
public class MyRandom {

    public static void main(String[] args) {
        MyRandom r = new MyRandom("hoten");
        System.out.println(r.nextDouble(0, 4));
        System.out.println(r.nextDouble(0, 4));
        System.out.println(r.nextDouble(0, 4));
        System.out.println(r.nextDouble(0, 4));
        System.out.println(r.nextDouble(0, 4));
        System.out.println(r.nextDouble(0, 4));

    }
    private int seed;

    public MyRandom(Object seed) {
        this.seed = seed.hashCode();
    }

    public MyRandom(int seed) {
        this.seed = seed;
    }

    public MyRandom() {
        seed = (int) (Math.random() * Integer.MAX_VALUE);
    }

    //[min,max)
    public double nextDouble(double min, double max) {
        seed = Math.abs((seed * 16807) % 2147483647);
        double r = min + (max - min) * (seed / 2147483647.0);
        return r;
    }

    //[min, max)
    public int nextInt(int min, int max) {
        seed = Math.abs((seed * 16807) % 2147483647);
        return min + (int) ((max - min) * (seed / 2147483647.0));
    }
}
