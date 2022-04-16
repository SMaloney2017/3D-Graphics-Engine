package graphicsengine;

import graphicsengine.structures.*;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.util.Comparator;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.ImageIcon;

import static java.lang.Math.*;

public class GraphicsEngine extends Canvas implements Runnable {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private final JFrame renderWindow;

    private Thread thread;
    private boolean active = false;
    private vec3d vCamera = new vec3d(0, 0, 0);

    private static final mat4x4 matProj = new mat4x4(); /* Projection matrix */
    private float fTheta;

    /* Test Cube */
    private static final mesh meshCube = new mesh();

    GraphicsEngine() {
        this.renderWindow = new JFrame();
        this.renderWindow.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        /* Create meshCube w/ triangles for dev testing
        meshCube.tris.add(new triangle(new vec3d(0, 0, 0), new vec3d(0, 1, 0), new vec3d(1, 1, 0)));
        meshCube.tris.add(new triangle(new vec3d(0, 0, 0), new vec3d(1, 1, 0), new vec3d(1, 0, 0)));

        meshCube.tris.add(new triangle(new vec3d(1, 0, 0), new vec3d(1, 1, 0), new vec3d(1, 1, 1)));
        meshCube.tris.add(new triangle(new vec3d(1, 0, 0), new vec3d(1, 1, 1), new vec3d(1, 0, 1)));

        meshCube.tris.add(new triangle(new vec3d(1, 0, 1), new vec3d(1, 1, 1), new vec3d(0, 1, 1)));
        meshCube.tris.add(new triangle(new vec3d(1, 0, 1), new vec3d(0, 1, 1), new vec3d(0, 0, 1)));

        meshCube.tris.add(new triangle(new vec3d(0, 0, 1), new vec3d(0, 1, 1), new vec3d(0, 1, 0)));
        meshCube.tris.add(new triangle(new vec3d(0, 0, 1), new vec3d(0, 1, 0), new vec3d(0, 0, 0)));

        meshCube.tris.add(new triangle(new vec3d(0, 1, 0), new vec3d(0, 1, 1), new vec3d(1, 1, 1)));
        meshCube.tris.add(new triangle(new vec3d(0, 1, 0), new vec3d(1, 1, 1), new vec3d(1, 1, 0)));

        meshCube.tris.add(new triangle(new vec3d(1, 0, 1), new vec3d(0, 0, 1), new vec3d(0, 0, 0)));
        meshCube.tris.add(new triangle(new vec3d(1, 0, 1), new vec3d(0, 0, 0), new vec3d(1, 0, 0)));
        */

        meshCube.LoadFromObjectFile("./src/resources/test.obj");

        float fNear = 0.1f;
        float fFar = 1000.0f;
        float fFov = 90.0f;
        float fAspectRatio = (float)HEIGHT / (float)WIDTH;
        float fFovRad = 1.0f / (float)tan(fFov * 0.5f / 180.0f * 3.14159f);

        matProj.m[0][0] = fAspectRatio * fFovRad;
        matProj.m[1][1] = fFovRad;
        matProj.m[2][2] = fFar / (fFar - fNear);
        matProj.m[3][2] = (-fFar * fNear) / (fFar - fNear);
        matProj.m[2][3] = 1.0f;
        matProj.m[3][3] = 0.0f;

    }

    public static void main(String[] args) {
        GraphicsEngine window = new GraphicsEngine();
        window.renderWindow.add(window);
        window.renderWindow.pack();
        window.renderWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.renderWindow.setResizable(false);
        window.renderWindow.setVisible(true);
        window.renderWindow.setIconImage(new ImageIcon("./src/resources/icon.png").getImage()); /* Replace JFrame icon with transparent 1x1 pixel */
        window.start();
    }

    public synchronized void start() {
        active = true;
        this.thread = new Thread(this, "GraphicsEngineWindow");
        this.thread.start();
    }

    public synchronized void stop() {
        active = false;
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 144.0; /* run at 144fps */
        float delta = 0;
        while(active) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while(delta >= 1){
                render(delta * 0.01f);
                delta--;
            }
        }
        stop();
    }

    private void render(float fElapsedTime) {
        BufferStrategy bufferStrategy = this.getBufferStrategy();
        if(bufferStrategy == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        mat4x4 matRotZ = new mat4x4();
        mat4x4 matRotX = new mat4x4();
        fTheta += fElapsedTime;

        matRotZ.m[0][0] = (float)cos(fTheta);
        matRotZ.m[0][1] = (float)sin(fTheta);
        matRotZ.m[1][0] = (float)-sin(fTheta);
        matRotZ.m[1][1] = (float)cos(fTheta);
        matRotZ.m[2][2] = 1;
        matRotZ.m[3][3] = 1;

        matRotX.m[0][0] = 1;
        matRotX.m[1][1] = (float)cos(fTheta * 0.5f);
        matRotX.m[1][2] = (float)sin(fTheta * 0.5f);
        matRotX.m[2][1] = (float)-sin(fTheta * 0.5f);
        matRotX.m[2][2] = (float)cos(fTheta * 0.5f);
        matRotX.m[3][3] = 1;

        Vector<triangle> vecTrianglesToRaster = new Vector<>();

        /* Loop to project triangles */
        for(triangle tri : meshCube.tris) {

            triangle triProjected = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));
            triangle triRotatedZ = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));
            triangle triRotatedZX = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));

            /* Rotate in Y-Axis */
            MultiplyMatrixVector(tri.points[0], triRotatedZ.points[0], matRotZ);
            MultiplyMatrixVector(tri.points[1], triRotatedZ.points[1], matRotZ);
            MultiplyMatrixVector(tri.points[2], triRotatedZ.points[2], matRotZ);

            /* Rotate in X-Axis */
            MultiplyMatrixVector(triRotatedZ.points[0], triRotatedZX.points[0], matRotX);
            MultiplyMatrixVector(triRotatedZ.points[1], triRotatedZX.points[1], matRotX);
            MultiplyMatrixVector(triRotatedZ.points[2], triRotatedZX.points[2], matRotX);

            /* Increase distance from camera */
            triangle triTranslated = triRotatedZX;
            triTranslated.points[0].z = triRotatedZX.points[0].z + 100.0f;
            triTranslated.points[1].z = triRotatedZX.points[1].z + 100.0f;
            triTranslated.points[2].z = triRotatedZX.points[2].z + 100.0f;

            /* Calculate normal of triangles */
            vec3d normal = new vec3d(0, 0, 0);
            vec3d line1 = new vec3d(0, 0, 0);
            vec3d line2 = new vec3d(0, 0, 0);
            line1.x = triTranslated.points[1].x - triTranslated.points[0].x;
            line1.y = triTranslated.points[1].y - triTranslated.points[0].y;
            line1.z = triTranslated.points[1].z - triTranslated.points[0].z;

            line2.x = triTranslated.points[2].x - triTranslated.points[0].x;
            line2.y = triTranslated.points[2].y - triTranslated.points[0].y;
            line2.z = triTranslated.points[2].z - triTranslated.points[0].z;

            normal.x = line1.y * line2.z - line1.z * line2.y;
            normal.y = line1.z * line2.x - line1.x * line2.z;
            normal.z = line1.x * line2.y - line1.y * line2.x;

            /* Normalize 'normal' vector */
            float normalLen = (float)sqrt(normal.x*normal.x + normal.y*normal.y + normal.z*normal.z);
            normal.x /= normalLen;
            normal.y /= normalLen;
            normal.z /= normalLen;

            /* Draw triangles facing the camera only */
            if(normal.x * (triTranslated.points[0].x - vCamera.x) +
               normal.y * (triTranslated.points[0].y - vCamera.y) +
               normal.z * (triTranslated.points[0].z - vCamera.z) < 0.0f) {

                /* Project 3D triangles -> 2D */
                MultiplyMatrixVector(triTranslated.points[0], triProjected.points[0], matProj);
                MultiplyMatrixVector(triTranslated.points[1], triProjected.points[1], matProj);
                MultiplyMatrixVector(triTranslated.points[2], triProjected.points[2], matProj);

                /* Scale to view */
                triProjected.points[0].x += 1.0f; triProjected.points[0].y += 1.0f;
                triProjected.points[1].x += 1.0f; triProjected.points[1].y += 1.0f;
                triProjected.points[2].x += 1.0f; triProjected.points[2].y += 1.0f;

                triProjected.points[0].x *= 0.5f * (float) WIDTH; triProjected.points[0].y *= 0.5f * (float) HEIGHT;
                triProjected.points[1].x *= 0.5f * (float) WIDTH; triProjected.points[1].y *= 0.5f * (float) HEIGHT;
                triProjected.points[2].x *= 0.5f * (float) WIDTH; triProjected.points[2].y *= 0.5f * (float) HEIGHT;

                /* Create vector of all triangles */
                vecTrianglesToRaster.add(triProjected);

            }
        }

        /* Sort triangles by average z value */
        vecTrianglesToRaster.sort(new TriangleComparator());

        /* Loop to draw triangles */
        for (triangle tri : vecTrianglesToRaster) {
            FillTriangle((int) tri.points[0].x, (int) tri.points[0].y,
                    (int) tri.points[1].x, (int) tri.points[1].y,
                    (int) tri.points[2].x, (int) tri.points[2].y,
                    graphics, Color.WHITE);

            DrawTriangle((int) tri.points[0].x, (int) tri.points[0].y,
                    (int) tri.points[1].x, (int) tri.points[1].y,
                    (int) tri.points[2].x, (int) tri.points[2].y,
                    graphics, Color.BLACK);
        }

        graphics.dispose();
        bufferStrategy.show();
    }

    private void MultiplyMatrixVector(vec3d inputVec, vec3d outputVec, mat4x4 matProj) {
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

    private void DrawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Graphics g, Color c) {
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x3, y3, x1, y1);
    }

    private void FillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Graphics g, Color c) {
        g.setColor(c);
        g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }

    static class TriangleComparator implements Comparator<triangle> {
        @Override
        public int compare(triangle t1, triangle t2) {
            float z1 = (t1.points[0].z + t1.points[1].z + t1.points[2].z) / 3.0f;
            float z2 = (t2.points[0].z + t2.points[1].z + t2.points[2].z) / 3.0f;

            return Float.compare(z2, z1);
        }
    }

}
