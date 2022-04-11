package graphicsengine;

import graphicsengine.structures.*;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;

import static java.lang.Math.*;

public class GraphicsEngine extends Canvas implements Runnable {
    private final JFrame renderWindow;
    private Thread thread;

    private static boolean active = false;

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    
    /* Test Cube */
    private static final mesh meshCube = new mesh();
    private static final mat4x4 matProj = new mat4x4();
    float fTheta;

    GraphicsEngine() {
        this.renderWindow = new JFrame();
        this.renderWindow.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        /* Create meshCube w/ triangles for dev testing */
        meshCube.tris.add(new triangle(new vec3d(0, 0, 0), new vec3d(0, 1, 0), new vec3d(1, 1, 0)));
        meshCube.tris.add(new triangle(new vec3d(0, 0, 1), new vec3d(1, 1, 0), new vec3d(1, 0, 0)));

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
        window.renderWindow.setTitle("3DGraphicsEngine");
        window.renderWindow.add(window);
        window.renderWindow.pack();
        window.renderWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.renderWindow.setResizable(false);
        window.renderWindow.setVisible(true);

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
        final double ns = 1000000000.0 / 144.0;
        float delta = 0;
        while(active) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while(delta >= 1){
                render(delta * 0.01f);
                delta--;
            }
            //render(delta / 1000);
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

        mat4x4 matRotZ = new mat4x4(), matRotX = new mat4x4();
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

        /* Loop to draw triangles */
        for(triangle tri : meshCube.tris) {
            /* Get projected triangle */
            triangle triProjected = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));
            triangle triRotatedZ = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));
            triangle triRotatedZX = new triangle(new vec3d(0, 0, 0), new vec3d(0, 0, 0), new vec3d(0, 0, 0));

            MultiplyMatrixVector(tri.points[0], triRotatedZ.points[0], matRotZ);
            MultiplyMatrixVector(tri.points[1], triRotatedZ.points[1], matRotZ);
            MultiplyMatrixVector(tri.points[2], triRotatedZ.points[2], matRotZ);

            MultiplyMatrixVector(triRotatedZ.points[0], triRotatedZX.points[0], matRotX);
            MultiplyMatrixVector(triRotatedZ.points[1], triRotatedZX.points[1], matRotX);
            MultiplyMatrixVector(triRotatedZ.points[2], triRotatedZX.points[2], matRotX);

            triangle triTranslated = triRotatedZX;
            triTranslated.points[0].z = triRotatedZX.points[0].z + 3.0f;
            triTranslated.points[1].z = triRotatedZX.points[1].z + 3.0f;
            triTranslated.points[2].z = triRotatedZX.points[2].z + 3.0f;

            MultiplyMatrixVector(triTranslated.points[0], triProjected.points[0], matProj);
            MultiplyMatrixVector(triTranslated.points[1], triProjected.points[1], matProj);
            MultiplyMatrixVector(triTranslated.points[2], triProjected.points[2], matProj);

            /* Scale to view */
            triProjected.points[0].x += 1.0f;
            triProjected.points[0].y += 1.0f;

            triProjected.points[1].x += 1.0f;
            triProjected.points[1].y += 1.0f;

            triProjected.points[2].x += 1.0f;
            triProjected.points[2].y += 1.0f;

            triProjected.points[0].x *= 0.5f * (float)WIDTH;
            triProjected.points[0].y *= 0.5f * (float)HEIGHT;

            triProjected.points[1].x *= 0.5f * (float)WIDTH;
            triProjected.points[1].y *= 0.5f * (float)HEIGHT;

            triProjected.points[2].x *= 0.5f * (float)WIDTH;
            triProjected.points[2].y *= 0.5f * (float)HEIGHT;

            DrawTriangle((int)triProjected.points[0].x, (int)triProjected.points[0].y,
                         (int)triProjected.points[1].x, (int)triProjected.points[1].y,
                         (int)triProjected.points[2].x, (int)triProjected.points[2].y,
                         graphics, Color.WHITE);

        }

        graphics.dispose();
        bufferStrategy.show();
    }

    private void MultiplyMatrixVector(vec3d i, vec3d o, mat4x4 m) {
        o.x = i.x * m.m[0][0] + i.y * m.m[1][0] + i.z * m.m[2][0] + m.m[3][0];
        o.y = i.x * m.m[0][1] + i.y * m.m[1][1] + i.z * m.m[2][1] + m.m[3][1];
        o.z = i.x * m.m[0][2] + i.y * m.m[1][2] + i.z * m.m[2][2] + m.m[3][2];
        float w = i.x * m.m[0][3] + i.y * m.m[1][3] + i.z * m.m[2][3] + m.m[3][3];

        if(w != 0.0f) {
            o.x /= w;
            o.y /= w;
            o.z /= w;
        }
    }

    private void DrawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Graphics g, Color c) {
        g.setColor(c);
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x3, y3, x1, y1);
    }
}
