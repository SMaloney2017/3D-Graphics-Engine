package graphics.utility;

import graphics.structures.mat4x4;
import graphics.structures.vec3d;

import static graphics.utility.VectorUtil.*;
import static java.lang.Math.*;

public class MatrixUtil {
    public static vec3d MatrixMultiplyVector(mat4x4 m, vec3d i) {
        float x = i.x * m.m[0][0] + i.y * m.m[1][0] + i.z * m.m[2][0] + i.w * m.m[3][0];
        float y = i.x * m.m[0][1] + i.y * m.m[1][1] + i.z * m.m[2][1] + i.w * m.m[3][1];
        float z = i.x * m.m[0][2] + i.y * m.m[1][2] + i.z * m.m[2][2] + i.w * m.m[3][2];
        float w = i.x * m.m[0][3] + i.y * m.m[1][3] + i.z * m.m[2][3] + i.w * m.m[3][3];
        return new vec3d(x, y, z, w);
    }

    public static mat4x4 MatrixMakeIdentity() {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 MatrixMakeRotationX(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = (float)cos(fAngleRad);
        matrix.m[1][2] = (float)sin(fAngleRad);
        matrix.m[2][1] = (float)-sin(fAngleRad);
        matrix.m[2][2] = (float)cos(fAngleRad);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 MatrixMakeRotationY(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float)cos(fAngleRad);
        matrix.m[0][2] = (float)sin(fAngleRad);
        matrix.m[2][0] = (float)-sin(fAngleRad);
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = (float)cos(fAngleRad);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 MatrixMakeRotationZ(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float)cos(fAngleRad);
        matrix.m[0][1] = (float)sin(fAngleRad);
        matrix.m[1][0] = (float)-sin(fAngleRad);
        matrix.m[1][1] = (float)cos(fAngleRad);
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 MatrixMakeTranslation(float x, float y, float z) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        matrix.m[3][0] = x;
        matrix.m[3][1] = y;
        matrix.m[3][2] = z;
        return matrix;
    }

    public static mat4x4 MatrixMakeProjection(float fFovDegrees, float fAspectRatio, float fNear, float fFar) {
        mat4x4 matrix = new mat4x4();
        float fFovRad = 1.0f / (float)tan(fFovDegrees * 0.5f / 180.0f * 3.14159f);
        matrix.m[0][0] = fAspectRatio * fFovRad;
        matrix.m[1][1] = fFovRad;
        matrix.m[2][2] = fFar / (fFar - fNear);
        matrix.m[3][2] = (-fFar * fNear) / (fFar - fNear);
        matrix.m[2][3] = 1.0f;
        matrix.m[3][3] = 0.0f;
        return matrix;
    }

    public static mat4x4 MatrixMultiplyMatrix(mat4x4 m1, mat4x4 m2) {
        mat4x4 matrix = new mat4x4();
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                matrix.m[r][c] = m1.m[r][0] * m2.m[0][c] + m1.m[r][1] * m2.m[1][c] + m1.m[r][2] * m2.m[2][c] + m1.m[r][3] * m2.m[3][c];
            }
        }
        return matrix;
    }

    public static mat4x4 MatrixPointAt(vec3d pos, vec3d target, vec3d up) {
        /* Calculate new forward direction */
        vec3d newForward = VectorSub(target, pos);
        newForward = VectorNormalize(newForward);

        /* Calculate new Up direction */
        vec3d a = VectorMul(newForward, VectorDotProduct(up, newForward));
        vec3d newUp = VectorSub(up, a);
        newUp = VectorNormalize(newUp);

        /* New Right direction is easy, its just cross product */
        vec3d newRight = VectorCrossProduct(newUp, newForward);

        /* Construct Dimensioning and Translation Matrix */
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = newRight.x;	matrix.m[0][1] = newRight.y;	matrix.m[0][2] = newRight.z;	matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = newUp.x;		matrix.m[1][1] = newUp.y;		matrix.m[1][2] = newUp.z;		matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = newForward.x;	matrix.m[2][1] = newForward.y;	matrix.m[2][2] = newForward.z;	matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = pos.x;			matrix.m[3][1] = pos.y;			matrix.m[3][2] = pos.z;			matrix.m[3][3] = 1.0f;
        return matrix;

    }

    /* Only for Rotation/Translation Matrices */
    public static mat4x4 MatrixQuickInverse(mat4x4 m) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = m.m[0][0]; matrix.m[0][1] = m.m[1][0]; matrix.m[0][2] = m.m[2][0]; matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = m.m[0][1]; matrix.m[1][1] = m.m[1][1]; matrix.m[1][2] = m.m[2][1]; matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = m.m[0][2]; matrix.m[2][1] = m.m[1][2]; matrix.m[2][2] = m.m[2][2]; matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = -(m.m[3][0] * matrix.m[0][0] + m.m[3][1] * matrix.m[1][0] + m.m[3][2] * matrix.m[2][0]);
        matrix.m[3][1] = -(m.m[3][0] * matrix.m[0][1] + m.m[3][1] * matrix.m[1][1] + m.m[3][2] * matrix.m[2][1]);
        matrix.m[3][2] = -(m.m[3][0] * matrix.m[0][2] + m.m[3][1] * matrix.m[1][2] + m.m[3][2] * matrix.m[2][2]);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }
}
