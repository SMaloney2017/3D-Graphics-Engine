package graphicsengine.structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Vector;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

public class mesh {
    public Vector<triangle> tris;

    public mesh() {
        tris = new Vector<>();
    }

    public boolean LoadFromObjectFile(String sFilename) {
        Vector<vec3d> vertices = new Vector<>();
        try {
            File myObj = new File(sFilename);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] tokens = data.split(" ");
                if (Objects.equals(tokens[0], "v")) {
                    vec3d vec = new vec3d(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
                    vertices.add(vec);
                }
                if (Objects.equals(tokens[0], "f")) {
                    int[] f = { parseInt(tokens[1]), parseInt(tokens[2]), parseInt(tokens[3]) };
                    tris.add(new triangle(vertices.get(f[0] - 1), vertices.get(f[1] - 1), vertices.get(f[2] - 1)));
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
            return false;
        }
        System.out.println(vertices);
        return true;
    }
}
