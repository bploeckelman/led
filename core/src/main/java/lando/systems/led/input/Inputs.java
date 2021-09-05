package lando.systems.led.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

public class Inputs {

    public static final Vector3 mouse_screen = new Vector3();
    public static final Vector3 mouse_world = new Vector3();

    public static WorldInput world_input;
    public static CameraInput camera_input;

    public static void init(WorldInput world_input, CameraInput camera_input) {
        Inputs.world_input = world_input;
        Inputs.camera_input = camera_input;
    }

    public static void update() {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera_input.camera.unproject(mouse_world);

        camera_input.pan_enabled  = !world_input.show_new_level_button;
        camera_input.zoom_enabled = !world_input.show_new_level_button;
    }

}
