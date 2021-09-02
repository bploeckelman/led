package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import lando.systems.led.utils.RectI;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final Vector3 touch_pos;
    final Vector3 mouse_pos;
    final MouseButtons mouse_buttons;
    final World world;

    public RectI selected_level;

    static class MouseButtons {
        boolean left_mouse_down;
        boolean middle_mouse_down;
        boolean right_mouse_down;
    }

    public WorldInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.touch_pos = new Vector3();
        this.mouse_pos = new Vector3();
        this.mouse_buttons = new MouseButtons();
        this.selected_level = null;
    }

    public void update(float dt) {
        mouse_pos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse_pos);

        if (selected_level != null) {
            if (mouse_buttons.left_mouse_down) {
                var grid_size = world.grid.getValue();

                // round appropriately to make sure the cell
                // that contains the mouse is always included
                // in the selection
                int grid_x = (mouse_pos.x <= 0f)
                        ? MathUtils.floor(mouse_pos.x / grid_size)
                        : MathUtils.ceil(mouse_pos.x / grid_size);
                int grid_y = (mouse_pos.y <= 0f)
                        ? MathUtils.floor(mouse_pos.y / grid_size)
                        : MathUtils.ceil(mouse_pos.y / grid_size);

                var selected_grid_x = (selected_level.x / grid_size);
                var selected_grid_y = (selected_level.y / grid_size);
                var w = (grid_x - selected_grid_x) * grid_size;
                var h = (grid_y - selected_grid_y) * grid_size;

                // require a minimum of 1 cell highlighted
                selected_level.w = (Math.abs(w) >= grid_size) ? w : grid_size;
                selected_level.h = (Math.abs(h) >= grid_size) ? h : grid_size;
            }
        }
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        switch (button) {
            case Input.Buttons.LEFT   -> mouse_buttons.left_mouse_down   = true;
            case Input.Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = true;
            case Input.Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = true;
        }
        touch_pos.set(x, y, 0);
        camera.unproject(touch_pos);

        if (selected_level == null && mouse_buttons.left_mouse_down) {
            var grid_size = world.grid.getValue();
            int grid_x = MathUtils.floor(mouse_pos.x / grid_size);
            int grid_y = MathUtils.floor(mouse_pos.y / grid_size);
            var clamp_x = grid_x * grid_size;
            var clamp_y = grid_y * grid_size;
            selected_level = RectI.of(clamp_x, clamp_y, grid_size, grid_size);
        }
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        switch (button) {
            case Input.Buttons.LEFT   -> mouse_buttons.left_mouse_down   = false;
            case Input.Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = false;
            case Input.Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = false;
        }
        if (button == Input.Buttons.LEFT) {
            world.add_level(selected_level);
            selected_level = null;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        mouse_pos.set(x, y, 0);
        camera.unproject(mouse_pos);
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        mouse_pos.set(x, y, 0);
        camera.unproject(mouse_pos);
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }

}
