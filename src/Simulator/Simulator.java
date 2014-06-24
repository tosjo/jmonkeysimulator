package Simulator;

//import com.bulletphysics.collision.shapes.CollisionShape;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.cinematic.events.MotionTrack;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Toby
 */
public class Simulator extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private Node testCarNode;
    private Node carNode;
    private Node[] carNodes;
    private Car[] cars;
    private MotionEvent motionControl;
    private RigidBodyControl landscape;
    MotionPath path = new MotionPath();
    MotionPath path2 = new MotionPath();
    MotionPathListener pathListener;
    Vector3f cameraPosition;
    String waypoints = "";
    PrintWriter out;
    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private Vector3f camStartLoc = new Vector3f(-250, 250, 250);
    String waypointsFromFile;
    String correctWPFromFile;
    String correctWPFromFile2;
    BitmapText camPos;
    BitmapText timerText;
    Vector3f flatLocation;
    float timer;
    Car car;
    Scanner in;
    int counter = 0;

    public static void main(String[] args) {
        Simulator app = new Simulator();
        app.start();
    }
    private VehicleControl vehicle;
    private VehicleControl player;
    private CinematicEventListener motionListener;

    public void simpleInitApp() {
        /**
         * Set up Physics
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);


        //customControl = new MyCustomControl(bulletAppState);


        cam.setLocation(camStartLoc);
        cam.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0));
        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpLight();
        initializeCar();
        setUpKeys();

        Quad quadMesh = new Quad(500, 500);
        quadMesh.updateGeometry(500, 500, false);
        quadMesh.updateBound();

        Geometry quad1 = new Geometry("Textured Quad", quadMesh);

        Texture tex = assetManager.loadTexture("Textures/CrossroadMapWithNumbers(METbeschrijvingen).png");
        tex.setWrap(Texture.WrapMode.Repeat);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        quad1.setMaterial(mat);

        quad1.setLocalTranslation(-250, 1, 250);
        quad1.rotate(-FastMath.PI / 2, 0, 0);

        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(quad1);
        landscape = new RigidBodyControl(sceneShape, 0);
        quad1.addControl(landscape);


        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        //rootNode.attachChild(quad1);

        timer = 0;
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private void initializeCar() {
        carNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");

        carNode.setShadowMode(ShadowMode.Cast);
        carNode.move(0, 1.4f, 0);
        //carNode.attachChild(carNode.clone());
        //carNode = null;

        //carNode.rotate(0, 1.2f, 0);
        //rootNode.attachChild(carNode);
    }

    private void drawText() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        camPos = new BitmapText(guiFont, false);
        camPos.setSize(guiFont.getCharSet().getRenderedSize());
        cameraPosition = new Vector3f(cam.getLocation());
        String camLocation = cameraPosition.toString();
        camPos.setText(camLocation);
        camPos.setLocalTranslation(300, camPos.getLineHeight(), 0);
        guiNode.attachChild(camPos);

        timerText = new BitmapText(guiFont, false);
        timerText.setSize(guiFont.getCharSet().getRenderedSize());
        timerText.setText("Time: " + String.valueOf(timer));
        timerText.setLocalTranslation(0, this.settings.getHeight(), 0);
        guiNode.attachChild(timerText);



        //addWaypoint(cameraPosition);
    }

    private void clearWayPoints() {
        try {
            path.clearWayPoints();
            path2.clearWayPoints();
            out = new PrintWriter("Waypoints\\waypoints.txt");
            out.print("");
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void addWaypoint(Vector3f waypointLocation) {
        flatLocation = waypointLocation;
        flatLocation.y = 0.1f;

//        path.addWayPoint(new Vector3f(waypointLocation.x+30,waypointLocation.y+30,waypointLocation.z+30));
        path.addWayPoint(waypointLocation);
        path2.addWayPoint(flatLocation);
        waypoints += flatLocation;
    }

    public void goToDestination() {

//        path.addWayPoint(new Vector3f(0, 0.5f, 50));
//        path.addWayPoint(new Vector3f(260, 0.5f, 50));
//        path.addWayPoint(new Vector3f(310, 0.5f, -10));
//        path.addWayPoint(new Vector3f(260, 0.5f, -88));
//        path.addWayPoint(new Vector3f(0, 0.5f, -88));


        if (flatLocation != null) {
            rootNode.attachChild(carNode);
            //carNode.attachChild(carNode.clone());

            motionControl = new MotionEvent(carNode, path);
            motionControl.setDirectionType(MotionEvent.Direction.PathAndRotation);
            motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.PI, Vector3f.UNIT_Y));
            //motionControl.setInitialDuration(20f);

            motionControl.setSpeed(3);
            motionControl.play();

            try {
                out = new PrintWriter("C:\\Users\\Toby\\Desktop\\jMonkeyProjects\\Simulator\\Waypoints\\waypoints.txt");
                out.print(waypoints);
                out.close();
                System.out.println(waypoints);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (flatLocation == null) {
            camPos.setText("No waypoints found!");
            System.out.println("No waypoints found!");
        }
    }

    public void addNewCar(int start, int end, int id) {
        final Car car = new Car(start, end, id, assetManager, bulletAppState);



        car.loadWaypoints();
        if (car.pathLoaded) {



            rootNode.attachChild(car.carNode);
            motionControl = new MotionEvent(car.carNode, car.getPath());
            motionControl.setDirectionType(MotionEvent.Direction.PathAndRotation);
            motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.PI, Vector3f.UNIT_Y));

            //motionControl.setInitialDuration(20f);

            //motionControl.setSpeed(3);
            motionControl.setSpeed(1 + (int) (Math.random() * ((4 - 1) + 1)));
            motionControl.play();
            car.getPath().addListener(new MotionPathListener() {
                public void onWayPointReach(MotionEvent control, int wayPointIndex) {
                    if (car.getPath().getNbWayPoints() == wayPointIndex + 1) {
                        //motionControl.stop();
                        car.delete(rootNode);
                        System.out.println("CAR DELETED");
                    } else {
                        System.out.println(control.getSpatial().getName() + car.id + " has reached way point " + wayPointIndex);
                    }
                }
            });


        } else {
            System.out.println("path didn't load");
        }
    }

    private void setUpKeys() {
        inputManager.addMapping("Start", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Start");

        inputManager.addMapping("Add", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(this, "Add");

        inputManager.addMapping("Load", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addListener(this, "Load");

        inputManager.addMapping("Clear", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addListener(this, "Clear");

        inputManager.addMapping("New", new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener(this, "New");

        inputManager.addMapping("New2", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(this, "New2");

        inputManager.addMapping("spawntestcar", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addListener(this, "spawntestcar");

    }

    @Override
    public void simpleUpdate(float tpf) {
        //drawText();
        timer += ((tpf));
    }

    public void loadWaypoints() {
        float x = 0;
        float y = 0;
        float z = 0;
        try {
            in = new Scanner(new FileReader("C:\\Users\\Toby\\Desktop\\jMonkeyProjects\\Simulator\\Waypoints\\waypoints16-13.txt"));
            waypointsFromFile = in.nextLine();
            correctWPFromFile = waypointsFromFile.replace("(", "");
            correctWPFromFile2 = correctWPFromFile.replace(" ", "");
            String formattedWayPoints[] = correctWPFromFile2.split("\\)");
            for (int i = 0; i < formattedWayPoints.length; i++) {
                String[] vectorWP = formattedWayPoints[i].split(",");
                x = Float.valueOf(vectorWP[0]);
                y = Float.valueOf(vectorWP[1]);
                z = Float.valueOf(vectorWP[2]);

                Vector3f test = new Vector3f(x, y, z);
                addWaypoint(test);
            }


        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }



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

    public void spawntestcar() {
        testCarNode = (Node) assetManager.loadModel("Models/Ferrari/Car.scene");
        testCarNode.setShadowMode(RenderQueue.ShadowMode.Cast);
        testCarNode.setLocalTranslation(-13, 1, 193);
        Geometry chasis = findGeom(testCarNode, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);
        player = new VehicleControl(carHull, 1);
        player.setKinematic(true);
        player.setMass(1f);
        testCarNode.addControl(player);
        player.setCollisionShape(CollisionShapeFactory.createDynamicMeshShape(findGeom(testCarNode, "Car")));
        
        rootNode.attachChild(testCarNode);

        System.out.println(testCarNode.getLocalTranslation().toString());

    }

    /**
     * These are our custom actions triggered by key presses.
     */
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Start")) {
            if (isPressed) {
                goToDestination();
            }
        } else if (binding.equals("Add")) {
            if (isPressed) {
                addWaypoint(cameraPosition);
            }
        } else if (binding.equals("Load")) {
            if (isPressed) {
                loadWaypoints();
            }
        } else if (binding.equals("Clear")) {
            if (isPressed) {
                clearWayPoints();
            }
        } else if (binding.equals("New")) {
            if (isPressed) {
                //maak nieuwe car met alle eigenschappen.
                addNewCar(16, 13, 0);
            }

        } else if (binding.equals("New2")) {
            if (isPressed) {
                //maak nieuwe car met alle eigenschappen.
                addNewCar(14, 6, 1);
            }
        } else if (binding.equals("spawntestcar")) {
            if (isPressed) {
                //maak nieuwe car met alle eigenschappen.
                spawntestcar();
            }

        }

    }
}