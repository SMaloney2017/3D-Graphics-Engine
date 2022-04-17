package graphicsengine.utility;

import graphicsengine.structures.mat4x4;
import graphicsengine.structures.triangle;
import graphicsengine.structures.vec3d;

import static java.lang.Math.sqrt;

public class VectorUtil {
    public static void MultiplyMatrixVector(vec3d inputVec, vec3d outputVec, mat4x4 matProj) {
        outputVec.x = inputVec.x * matProj.m[0][0] + inputVec.y * matProj.m[1][0] + inputVec.z * matProj.m[2][0] + matProj.m[3][0];
        outputVec.y = inputVec.x * matProj.m[0][1] + inputVec.y * matProj.m[1][1] + inputVec.z * matProj.m[2][1] + matProj.m[3][1];
        outputVec.z = inputVec.x * matProj.m[0][2] + inputVec.y * matProj.m[1][2] + inputVec.z * matProj.m[2][2] + matProj.m[3][2];
        float w = inputVec.x * matProj.m[0][3] + inputVec.y * matProj.m[1][3] + inputVec.z * matProj.m[2][3] + matProj.m[3][3];

        if(w != 0.0f) {
            outputVec.x /= w;
            outputVec.y /= w;
            outputVec.z /= w;
        }
    }

    public static vec3d VectorAdd(vec3d v1, vec3d v2) {
        return new vec3d(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    public static vec3d VectorSub(vec3d v1, vec3d v2) {
        return new vec3d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    public static vec3d VectorMul(vec3d v1, float k) {
        return new vec3d(v1.x * k, v1.y * k, v1.z * k);
    }

    public static vec3d DivVector(vec3d v1, float k) {
        return new vec3d(v1.x / k, v1.y / k, v1.z / k);
    }

    public static float VectorDotProduct(vec3d v1, vec3d v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public static float VectorLength(vec3d v1) {
        return (float)sqrt(VectorDotProduct(v1, v1));
    }

    public static vec3d VectorNormalize(vec3d v1) {
        float l = VectorLength(v1);
        return new vec3d(v1.x / l, v1.y / l, v1.z / l);
    }

    public static vec3d VectorCrossProduct(vec3d v1, vec3d v2) {
        return new vec3d(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x);
    }

    public static vec3d VectorIntersectPlane(vec3d plane_p, vec3d plane_n, vec3d lineStart, vec3d lineEnd) {
        plane_n = VectorNormalize(plane_n);
        float plane_d = -VectorDotProduct(plane_n, plane_p);
        float ad = VectorDotProduct(lineStart, plane_n);
        float bd = VectorDotProduct(lineEnd, plane_n);
        float t = (-plane_d - ad) / (bd - ad);
        vec3d lineStartToEnd = VectorSub(lineEnd, lineStart);
        vec3d lineToIntersect = VectorMul(lineStartToEnd, t);
        return VectorAdd(lineStart, lineToIntersect);
    }

    public static float distance(vec3d plane_n, vec3d plane_p, vec3d p) {
        vec3d n = VectorNormalize(p);
        return (plane_n.x * p.x + plane_n.y * p.y + plane_n.z * p.z - VectorDotProduct(plane_n, plane_p));
    }

    public static int ClipAgainstPlaneTriangle(vec3d plane_p, vec3d plane_n, triangle in_tri, triangle out_tri1, triangle out_tri2) {
        /* Make sure plane normal is indeed normal */
        plane_n = VectorNormalize(plane_n);

        /* Return signed shortest distance from point to plane, plane normal must be normalized */

        /* Create two temporary storage arrays to classify points either side of plane
           If distance sign is positive, point lies on "inside" of plane */
        vec3d[] inside_points = new vec3d[3];  int nInsidePointCount = 0;
        vec3d[] outside_points = new vec3d[3]; int nOutsidePointCount = 0;

        /* Get signed distance of each point in triangle to plane */
        float d0 = distance(plane_n, plane_p, in_tri.points[0]);
        float d1 = distance(plane_n, plane_p, in_tri.points[1]);
        float d2 = distance(plane_n, plane_p, in_tri.points[2]);

        if (d0 >= 0) { inside_points[nInsidePointCount++] = in_tri.points[0]; }
        else { outside_points[nOutsidePointCount++] = in_tri.points[0]; }
        if (d1 >= 0) { inside_points[nInsidePointCount++] = in_tri.points[1]; }
        else { outside_points[nOutsidePointCount++] = in_tri.points[1]; }
        if (d2 >= 0) { inside_points[nInsidePointCount++] = in_tri.points[2]; }
        else { outside_points[nOutsidePointCount++] = in_tri.points[2]; }

        /* Now classify triangle points, and break the input triangle into
           smaller output triangles if required. There are four possible
           outcomes... */

        if (nInsidePointCount == 0) {
            /* All points lie on the outside of plane, so clip whole triangle
               It ceases to exist */

            return 0; /* No returned triangles are valid */
        } else if (nInsidePointCount == 3) {
            /* All points lie on the inside of plane, so do nothing
               and allow the triangle to simply pass through */
            out_tri1 = in_tri;

            return 1; /* Just the one returned original triangle is valid */
        }else if (nInsidePointCount == 1 && nOutsidePointCount == 2) {
            /* Triangle should be clipped. As two points lie outside
               the plane, the triangle simply becomes a smaller triangle */

            /* Copy appearance info to new triangle
            out_tri1.color =  in_tri.col;
            out_tri1.sym = in_tri.sym; */

            /* The inside point is valid, so keep that... */
            out_tri1.points[0] = inside_points[0];

            /* but the two new points are at the locations where the
               original sides of the triangle (lines) intersect with the plane */
            out_tri1.points[1] = VectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);
            out_tri1.points[2] = VectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[1]);

            return 1; /* Return the newly formed single triangle */
        }else if (nInsidePointCount == 2 && nOutsidePointCount == 1) {
            /* Triangle should be clipped. As two points lie inside the plane,
               the clipped triangle becomes a "quad". Fortunately, we can
               represent a quad with two new triangles */

            /* Copy appearance info to new triangles
            out_tri1.col =  in_tri.col;
            out_tri1.sym = in_tri.sym;

            out_tri2.col =  in_tri.col;
            out_tri2.sym = in_tri.sym; */

            /* The first triangle consists of the two inside points and a new
               point determined by the location where one side of the triangle
               intersects with the plane */
            out_tri1.points[0] = inside_points[0];
            out_tri1.points[1] = inside_points[1];
            out_tri1.points[2] = VectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);

            /* The second triangle is composed of one of he inside points, a
               new point determined by the intersection of the other side of the
               triangle and the plane, and the newly created point above */
            out_tri2.points[0] = inside_points[1];
            out_tri2.points[1] = out_tri1.points[2];
            out_tri2.points[2] = VectorIntersectPlane(plane_p, plane_n, inside_points[1], outside_points[0]);

            return 2; /* Return two newly formed triangles which form a quad */
        }

        return 0;
    }
}
