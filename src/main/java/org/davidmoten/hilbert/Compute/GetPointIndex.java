package org.davidmoten.hilbert.Compute;

import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import java.math.BigInteger;

public class GetPointIndex {
    public static void main(String[] args) {
        int order = 3; // 希尔伯特曲线的阶数
        int dimension = 2; // 2D 空间
        // 测试点集
        long[][] points = {
                {0, 0},
                {1, 2},
                {9, 17},
                {18999, 11799}
        };
        int i = 1;
        HilbertCurve c = HilbertCurve.bits(order).dimensions(dimension);
        BigInteger pointHilbertIndex = c.index(points[i]);
        System.out.println("点: [" + points[i][0] + "," + points[i][1] + "] -> 希尔伯特值: " + pointHilbertIndex);
    }
}
