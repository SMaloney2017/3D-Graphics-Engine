package graphics;

import graphics.structures.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.ImageIcon;

import static graphics.utility.VectorUtil.*;
import static graphics.utility.MatrixUtil.*;

public class GraphicsEngine extends Canvas implements Runnable {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private final JFrame renderWindow;

    private Thread thread;
    private boolean active = false;

    private vec3d vCamera = new vec3d(0.0f, 0.0f, 0.0f);
    private vec3d vLookDir = new vec3d(0.0f, 0.0f, 1.0f);

    private static mat4x4 matProj; /* Projection matrix */
    private float fYaw;
    private float fTheta;

    /* Object to be rendered */
    private static final mesh meshObj = new mesh();

    GraphicsEngine() {
        this.renderWindow = new JFrame();
        this.renderWindow.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        meshObj.LoadFromObjectFile("./src/graphics/resources/teapot.obj");
        // meshObj.LoadFromObjectFile("./src/graphics/resources/magnolia.obj");

        matProj = MatrixMakeProjection(90.0f, (float)HEIGHT / (float)WIDTH, 0.1f, 1000.0f);

    }

    public static void main(String[] args) {
        GraphicsEngine window = new GraphicsEngine();
        window.renderWindow.add(window);
        window.renderWindow.pack();
        window.renderWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.renderWindow.setResizable(false);
        window.renderWindow.setVisible(true);
        window.renderWindow.setIconImage(new ImageIcon("./src/graphics/resources/icon.png").getImage()); /* Replace JFrame icon with transparent 1x1 pixel */
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
                render(0.01f);
                delta--;
            }
        }
        stop();
    }

    private void render(float fElapsedTime) {

        /* Controls */
        vec3d vForward = VectorMul(vLookDir, 8.0f * fElapsedTime);

        if (Keyboard.isKeyPressed(KeyEvent.VK_UP)) {
            vCamera.y -= 8.0f * fElapsedTime;
        }

        if (Keyboard.isKeyPressed(KeyEvent.VK_DOWN)) {
            vCamera.y += 8.0f * fElapsedTime;
        }

        if (Keyboard.isKeyPressed(KeyEvent.VK_W)) {
            vCamera = VectorAdd(vCamera, vForward);
        }

        if (Keyboard.isKeyPressed(KeyEvent.VK_S)) {
            vCamera = VectorSub(vCamera, vForward);
        }

        if (Keyboard.isKeyPressed(KeyEvent.VK_A)) {
            fYaw += 2.0 * fElapsedTime;
        }

        if (Keyboard.isKeyPressed(KeyEvent.VK_D)) {
            fYaw -= 2.0 * fElapsedTime;
        }

        BufferStrategy bufferStrategy = this.getBufferStrategy();
        if(bufferStrategy == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        /* Rotation matrices */
        mat4x4 matRotZ, matRotX;
        fTheta += fElapsedTime;

        matRotZ = MatrixMakeRotationZ(fTheta * 0.5f);
        matRotX = MatrixMakeRotationX(fTheta);

        mat4x4 matWorld;
        mat4x4 matTrans = MatrixMakeTranslation(0.0f, 0.0f, 5.0f);

        matWorld = MatrixMultiplyMatrix(matRotZ, matRotX); /* Apply rotation matrices */
        matWorld = MatrixMultiplyMatrix(matWorld, matTrans); /* Apply translation matrices */

        vec3d vUp = new vec3d(0.0f, 1.0f, 0.0f);
        vec3d vTarget = new vec3d(0.0f, 0.0f, 1.0f);

        mat4x4 matCameraRot = MatrixMakeRotationY(fYaw);
        vLookDir = MatrixMultiplyVector(matCameraRot, vTarget);

        vTarget = VectorAdd(vCamera, vLookDir);

        mat4x4 matCamera = MatrixPointAt(vCamera, vTarget, vUp);

        mat4x4 matView = MatrixQuickInverse(matCamera);

        Vector<triangle> vecTrianglesToRaster = new Vector<>();

        /* Loop to project triangles */
        for(triangle tri : meshObj.tris) {

            triangle triProjected = new triangle(new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f));
            triangle triTransformed = new triangle(new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f));
            triangle triViewed = new triangle(new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 0.0f, 0.0f));


            triTransformed.points[0] = MatrixMultiplyVector(matWorld, tri.points[0]);
            triTransformed.points[1] = MatrixMultiplyVector(matWorld, tri.points[1]);
            triTransformed.points[2] = MatrixMultiplyVector(matWorld, tri.points[2]);

            /* Calculate normal of triangles */
            vec3d normal, line1, line2;

            line1 = VectorSub(triTransformed.points[1], triTransformed.points[0]);
            line2 = VectorSub(triTransformed.points[2], triTransformed.points[0]);

            normal = VectorCrossProduct(line1, line2);
            normal = VectorNormalize(normal);

            vec3d vCameraRay = VectorSub(triTransformed.points[0], vCamera);

            /* Draw triangles facing the camera only */
            if(VectorDotProduct(normal, vCameraRay) < 0.0f) {

                /* Convert world space -> view space */
                triViewed.points[0] = MatrixMultiplyVector(matView, triTransformed.points[0]);
                triViewed.points[1] = MatrixMultiplyVector(matView, triTransformed.points[1]);
                triViewed.points[2] = MatrixMultiplyVector(matView, triTransformed.points[2]);

                /* Clip viewed triangles against near plane */
                int nClippedTriangles;
                triangle triClip1 = new triangle();
                triangle triClip2 = new triangle();
                triangle[] triClipped = TriangleClipAgainstPlane(new vec3d(0.0f, 0.0f, 0.1f), new vec3d(0.0f, 0.0f, 1.0f), triViewed, triClip1, triClip2);
                nClippedTriangles = triClipped.length;

                for (int n = 0; n < nClippedTriangles; n++) {
                    /* Project 3D triangles -> 2D */
                    triProjected.points[0] = MatrixMultiplyVector(matProj, triClipped[n].points[0]);
                    triProjected.points[1] = MatrixMultiplyVector(matProj, triClipped[n].points[1]);
                    triProjected.points[2] = MatrixMultiplyVector(matProj, triClipped[n].points[2]);

                    triProjected.points[0] = VectorDiv(triProjected.points[0], triProjected.points[0].w);
                    triProjected.points[1] = VectorDiv(triProjected.points[1], triProjected.points[1].w);
                    triProjected.points[2] = VectorDiv(triProjected.points[2], triProjected.points[2].w);

                    /* Scale to view */
                    vec3d vOffsetView = new vec3d(1, 1, 0);
                    triProjected.points[0] = VectorAdd(triProjected.points[0], vOffsetView);
                    triProjected.points[1] = VectorAdd(triProjected.points[1], vOffsetView);
                    triProjected.points[2] = VectorAdd(triProjected.points[2], vOffsetView);

                    triProjected.points[0].x *= 0.5f * (float) WIDTH; triProjected.points[0].y *= 0.5f * (float) HEIGHT;
                    triProjected.points[1].x *= 0.5f * (float) WIDTH; triProjected.points[1].y *= 0.5f * (float) HEIGHT;
                    triProjected.points[2].x *= 0.5f * (float) WIDTH; triProjected.points[2].y *= 0.5f * (float) HEIGHT;

                    /* Create vector of all triangles */
                    vecTrianglesToRaster.add(triProjected);
                }
            }
        }

        /* Sort triangles by average Z value */
        vecTrianglesToRaster.sort(new TriangleComparator());

        /* Loop to draw triangles */
        for (triangle triToRaster : vecTrianglesToRaster) {
            /* Clip triangles against edges of viewing window */
            triangle[] triClipped = new triangle[2];
            Queue<triangle> listTriangles = new LinkedList<>();

            /* Add initial triangle */
            listTriangles.add(triToRaster);
            int nNewTriangles = 1;
            for (int i = 0; i < 4; i++) {
                int nTrisToAdd = 0;
                while (nNewTriangles > 0) {
                    triangle triClip1 = new triangle();
                    triangle triClip2 = new triangle();

                    triangle front = listTriangles.peek();
                    listTriangles.poll();
                    nNewTriangles = nNewTriangles - 1;
                    /* For each window boarder */
                    switch(i) {
                        case 0: triClipped = TriangleClipAgainstPlane(new vec3d(0.0f, 0.0f, 0.0f), new vec3d(0.0f, 1.0f, 0.0f), front, triClip1, triClip2);
                                nTrisToAdd = triClipped.length;
                                break;
                        case 1: triClipped = TriangleClipAgainstPlane(new vec3d(0.0f, (float)HEIGHT - 1, 0.0f), new vec3d(0.0f, -1.0f, 0.0f), front, triClip1, triClip2);
                                nTrisToAdd = triClipped.length;
                                break;
                        case 2: triClipped = TriangleClipAgainstPlane(new vec3d(0.0f, 0.0f, 0.0f), new vec3d(1.0f, 0.0f, 0.0f), front, triClip1, triClip2);
                                nTrisToAdd = triClipped.length;
                                break;
                        case 3: triClipped = TriangleClipAgainstPlane(new vec3d((float)WIDTH - 1, 0.0f, 0.0f), new vec3d(-1.0f, 0.0f, 0.0f), front, triClip1, triClip2);
                                nTrisToAdd = triClipped.length;
                                break;
                    }
                    listTriangles.addAll(Arrays.asList(triClipped).subList(0, nTrisToAdd));
                }
                nNewTriangles = listTriangles.size();
            }

            for(triangle tri: listTriangles) {

                /* Draw outline of triangle */
                DrawTriangle((int) tri.points[0].x, (int) tri.points[0].y,
                        (int) tri.points[1].x, (int) tri.points[1].y,
                        (int) tri.points[2].x, (int) tri.points[2].y,
                        graphics, Color.WHITE);

                /* Draw filled triangle */
                FillTriangle((int) tri.points[0].x, (int) tri.points[0].y,
                        (int) tri.points[1].x, (int) tri.points[1].y,
                        (int) tri.points[2].x, (int) tri.points[2].y,
                        graphics, Color.BLACK);
            }

        }

        graphics.dispose();
        bufferStrategy.show();
    }

    /* Draw graphics */
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

    /* Utility */
    static class TriangleComparator implements Comparator<triangle> {
        @Override
        public int compare(triangle t1, triangle t2) {
            float z1 = (t1.points[0].z + t1.points[1].z + t1.points[2].z) / 3.0f;
            float z2 = (t2.points[0].z + t2.points[1].z + t2.points[2].z) / 3.0f;

            return Float.compare(z2, z1);
        }
    }

    static class Keyboard {

        private static final Map<Integer, Boolean> pressedKeys = new HashMap<>();

        static {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
                synchronized (Keyboard.class) {
                    if (event.getID() == KeyEvent.KEY_PRESSED) pressedKeys.put(event.getKeyCode(), true);
                    else if (event.getID() == KeyEvent.KEY_RELEASED) pressedKeys.put(event.getKeyCode(), false);
                    return false;
                }
            });
        }

        public static boolean isKeyPressed(int keyCode) {
            return pressedKeys.getOrDefault(keyCode, false);
        }
    }

}
