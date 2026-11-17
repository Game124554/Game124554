package dev.stumble.physics;

/** A growable flat list of axis-aligned boxes: minX, minY, minZ, maxX, maxY, maxZ. */
public final class BoxList {
    public double[] data = new double[6 * 64];
    public int size;

    public void clear() {
        size = 0;
    }

    public void add(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int k = size * 6;
        if (k + 6 > data.length) {
            double[] grown = new double[data.length * 2];
            System.arraycopy(data, 0, grown, 0, k);
            data = grown;
        }
        data[k] = minX;
        data[k + 1] = minY;
        data[k + 2] = minZ;
        data[k + 3] = maxX;
        data[k + 4] = maxY;
        data[k + 5] = maxZ;
        size++;
    }
}
