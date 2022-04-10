package graphicsengine;

import graphicsengine.structures.*;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;

public class GraphicsEngineDisplay extends Canvas implements Runnable {
    private final JFrame renderWindow;
    private Thread thread;

    private static boolean active = false;

    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    
    /* Test Cube */
    private static final mesh meshCube = new mesh();

    GraphicsEngineDisplay() {
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

    }

    public static void main(String[] args) {
        GraphicsEngineDisplay window = new GraphicsEngineDisplay();
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
        final double ns = 1000000000.0 / 60.0;
        double delta = 0;
        while(active) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while(delta >= 1){
                update();
                delta--;
            }
            render();
        }
        stop();
    }

    private void render() {
        BufferStrategy bufferStrategy = this.getBufferStrategy();
        if(bufferStrategy == null) {
            this.createBufferStrategy(3);
            return;
        }
        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.dispose();
        bufferStrategy.show();
    }

    private void update() {

    }
}
