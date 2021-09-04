package lando.systems.led.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.github.xpenatan.imgui.ImGui;
import lando.systems.led.utils.Point;
import lando.systems.led.world.Level;
import lando.systems.led.world.World;

import static lando.systems.led.world.Level.DragHandle.Dir.*;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final MouseButtons mouse_buttons;
    final World world;

    // TODO: put in a global input class?
    final Vector3 touch_screen;
    final Vector3 touch_world;

    // TODO: temporarily exposed until moved to a global input class
    public final Vector3 mouse_screen;
    public final Vector3 mouse_world;

    private Level.DragHandle active_handle;

    // exposed because it impacts camera input, handled in Main
    public boolean show_new_level_button;

    public Point new_level_pos;

    static class MouseButtons {
        boolean left_mouse_down;
        boolean middle_mouse_down;
        boolean right_mouse_down;
    }

    public WorldInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.touch_screen = new Vector3();
        this.touch_world = new Vector3();
        this.mouse_screen = new Vector3();
        this.mouse_world = new Vector3();
        this.mouse_buttons = new MouseButtons();
        this.active_handle = null;
        this.show_new_level_button = false;
    }

    public void update(float dt) {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera.unproject(mouse_world);

        var active_level = world.get_active_level();
        if (active_level != null) {
            for (var handle : active_level.drag_handles.values()) {
                // update hover state for rendering
                handle.hovered = handle.circle.contains(mouse_world.x, mouse_world.y)
                              || handle == active_handle;
                // update effective radius to maintain consistent size on screen
                handle.circle.radius = handle.world_radius * camera.zoom;
            }
            active_level.update_handles();

            if (!show_new_level_button) {
                if (mouse_buttons.left_mouse_down && active_handle != null) {
                    if (active_handle.dir == left)  active_level.set_left_bound(mouse_world.x);
                    if (active_handle.dir == right) active_level.set_right_bound(mouse_world.x);
                    if (active_handle.dir == up)    active_level.set_up_bound(mouse_world.y);
                    if (active_handle.dir == down)  active_level.set_down_bound(mouse_world.y);
                }
            }
        }
    }

    public void build_imgui_data() {
        if (show_new_level_button) {
            var button_w = 200;
            var button_h = 40;
            var button_x = touch_screen.x;
            var button_y = touch_screen.y;
            ImGui.SetNextWindowBgAlpha(0.5f);
            ImGui.SetNextWindowPos(button_x, button_y);
            ImGui.Begin("New Level");
            {
                if (ImGui.Button("Add Level", button_w, button_h))
                {
                    // create a new level with default size at current location
                    var level = new Level(new_level_pos);
                    world.add_level(level);

                    hide_new_level_button();
                }
            }
            ImGui.End();
        }
    }

    private boolean hide_new_level_button() {
        boolean did_hide = show_new_level_button;
        show_new_level_button = false;
        if (new_level_pos != null) {
            Point.pool.free(new_level_pos);
        }
        return did_hide;
    }

    // ------------------------------------------------------------------------
    // InputAdapter implementation

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        touch_screen.set(x, y, 0);
        touch_world.set(touch_screen);
        camera.unproject(touch_world);

        switch (button) {
            case Buttons.LEFT   -> mouse_buttons.left_mouse_down   = true;
            case Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = true;
            case Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = true;
        }

        if (button == Buttons.RIGHT) {
            show_new_level_button = !show_new_level_button;

            if (show_new_level_button) {
                new_level_pos = Point.pool.obtain().set((int) touch_world.x, (int) touch_world.y);
            } else {
                hide_new_level_button();
            }
            return true;
        }
        else if (button == Buttons.MIDDLE) {
            hide_new_level_button();
            return true;
        }
        else if (button == Buttons.LEFT) {
            var dismissed_button = hide_new_level_button();
            if (!dismissed_button) {
                var touched_handle = false;
                var active_level = world.get_active_level();
                if (active_level != null) {
                    // check for drag handle touch
                    for (var handle : active_level.drag_handles.values()) {
                        if (handle.circle.contains(touch_world.x, touch_world.y)) {
                            active_handle = handle;
                            touched_handle = true;
                            break;
                        }
                    }
                }

                if (!touched_handle) {
                    // check for touch on a different level
                    var clicked_level = world.pick_level_at((int) touch_world.x, (int) touch_world.y);
                    if (clicked_level != null && !world.is_active(clicked_level)) {
                        world.make_active(clicked_level);
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        switch (button) {
            case Buttons.LEFT   -> mouse_buttons.left_mouse_down   = false;
            case Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = false;
            case Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = false;
        }

        if (button == Buttons.LEFT) {
            if (active_handle != null) {
                active_handle = null;
            }
        }

        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.DEL
         || keycode == Keys.FORWARD_DEL) {
            // TODO: prompt before deleting
            world.delete_active_level();
            return true;
        }
        return false;
    }

}
