/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hoten.geom;

/**
 * GenUtil.java Function Date Jun 17, 2013
 *
 * @author Connor
 */
public class GenUtils {

    public static boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }
}
