package dev.stumble.physics;

/** What the solver needs to know about the world. Keeps the physics free of game classes. */
public interface Environment {
    /** Add every solid box that touches the given region to {@code out}. */
    void collectBoxes(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, BoxList out);

    /**
     * The fluid at a point, if any.
     *
     * @param flow receives {flowX, flowY, flowZ, isLava ? 1 : 0}; the flow is the direction the
     *             current runs, length about 1, or zero in still fluid
     * @return the height of the fluid surface above that point, or NaN when the point is not in
     *         or just above a fluid
     */
    double fluidSurface(double x, double y, double z, double[] flow);
}
