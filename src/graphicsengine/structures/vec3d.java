package graphicsengine.structures;

public class vec3d {
    public float x, y, z, w = 1;

    public vec3d(float i, float i1, float i2) {
        this.x = i; this.y = i1; this.z = i2;
    }

    public vec3d(float i, float i1, float i2, float i3) {
        this.x = i; this.y = i1; this.z = i2; this.w = i3;
    }
}

