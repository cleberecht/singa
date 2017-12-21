package de.bioforscher.singa.mathematics.geometry.bodies;

import de.bioforscher.singa.mathematics.vectors.Vector3D;

/**
 * @author cl
 */
public class Sphere {

    private final Vector3D center;
    private final double radius;

    public Sphere(Vector3D center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public Vector3D getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public double getVolume() {
        return 4.0/3.0*Math.PI*radius*radius*radius;
    }

    @Override
    public String toString() {
        return "Sphere{" +
                "center=" + center +
                ", radius=" + radius +
                '}';
    }
}
