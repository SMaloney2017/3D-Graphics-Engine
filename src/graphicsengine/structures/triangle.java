package graphicsengine.structures;

import java.awt.Graphics;

public class triangle {
    public vec3d[] points;

    public triangle(vec3d... points) {
        this.points = new vec3d[3];
        for(int i = 0; i < points.length; i++) {
            vec3d p = points[i];
            this.points[i] = new vec3d(p.x, p.y, p.z);
        }
    }
}
