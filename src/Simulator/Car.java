/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Simulator;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.cinematic.MotionPath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Toby
 */
public class Car {

    public boolean pathLoaded;
    private int startPoint;
    private int endPoint;
    private MotionPath path = new MotionPath();
    public int id;
    public Node carNode;
    private Vector3f flatLocation;
    private String correctWPFromFile2;
    private String waypointsFromFile;
    private String correctWPFromFile;
    private String generatedfilename;
    AssetManager assetManager;
    public VehicleControl player;

    public BulletAppState bulletAppState;

    public Car(int startPoint, int endPoint, int id, AssetManager manager, BulletAppState bulletAppState) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.id = id;
        this.assetManager=manager;
        this.bulletAppState = bulletAppState;
        
        generatedfilename = "C:\\Users\\Toby\\Desktop\\jMonkeyProjects\\Simulator\\Waypoints\\waypoints"+startPoint+"-"+endPoint+".txt";
        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(RenderQueue.ShadowMode.Cast);
        Geometry chasis = findGeom(carNode, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);
        player = new VehicleControl(carHull, 1);
        carNode.addControl(player);
        player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(carNode, "Car")));
        player.setKinematic(true);
        getPhysicsSpace().add(player);
        
    }
    
     private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    
    public Geometry findGeom(Spatial spatial, String name) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getQuantity(); i++) {
                Spatial child = node.getChild(i);
                Geometry result = findGeom(child, name);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry) {
            if (spatial.getName().startsWith(name)) {
                return (Geometry) spatial;
            }
        }
        return null;
    }

    public MotionPath getPath() {
        //loadWaypoints();
        if (pathLoaded) {
            System.out.println("Path is loaded");
            return this.path;
        } else {
            System.out.println("no path in car class yet. loading it now");
            return null;
        }

    }


    public void loadWaypoints() {
        pathLoaded = false;
        float x = 0;
        float y = 0;
        float z = 0;
        try {
            Scanner in = new Scanner(new FileReader(generatedfilename));
            waypointsFromFile = in.nextLine();
            correctWPFromFile = waypointsFromFile.replace("(", "");
            correctWPFromFile2 = correctWPFromFile.replace(" ", "");
            String formattedWayPoints[] = correctWPFromFile2.split("\\)");
            System.out.println(correctWPFromFile2);
            for (int i = 0; i < formattedWayPoints.length; i++) {
                String[] vectorWP = formattedWayPoints[i].split(",");
                x = Float.valueOf(vectorWP[0]);
                y = Float.valueOf(vectorWP[1]);
                z = Float.valueOf(vectorWP[2]);
                System.out.println(x + y + z);
                Vector3f test = new Vector3f(x, y, z);
                addWP(test);
            }
            pathLoaded = true;


        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("x = " + x);

    }

    private void addWP(Vector3f waypointLocation) {
        flatLocation = waypointLocation;
        flatLocation.y = 0.2f;

        path.addWayPoint(flatLocation);
    }
    
    public void delete(Node rootNode)
    {
        this.carNode.removeControl(player);
        this.carNode.removeFromParent();
        player.destroy();
        rootNode.detachChild(this.carNode);
        this.assetManager = null;
        this.bulletAppState = null;
        this.carNode = null;
        this.path = null;
        this.player = null;
        
    }
    
}
