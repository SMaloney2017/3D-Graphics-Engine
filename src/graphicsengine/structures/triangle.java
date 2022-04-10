package graphicsengine.structures;
public class triangle {
    vec3d[] points = new vec3d[3];

    public triangle(vec3d i, vec3d i1, vec3d i2) {
        this.points[0] = i;
        this.points[1] = i1;
        this.points[2] = i2;
    }
}
