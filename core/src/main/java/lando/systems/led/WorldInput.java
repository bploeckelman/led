package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

public class WorldInput extends InputAdapter {

    final OrthographicCamera camera;
    final Vector3 touch_pos;
    final Vector3 mouse_pos;
    final MouseButtons mouse_buttons;

    static class MouseButtons {
        boolean left_mouse_down;
        boolean middle_mouse_down;
        boolean right_mouse_down;
    }

    public WorldInput(OrthographicCamera camera) {
        this.camera = camera;
        this.touch_pos = new Vector3();
        this.mouse_pos = new Vector3();
        this.mouse_buttons = new MouseButtons();
    }

    public void update(float dt) {

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
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        switch (button) {
            case Input.Buttons.LEFT   -> mouse_buttons.left_mouse_down   = false;
            case Input.Buttons.MIDDLE -> mouse_buttons.middle_mouse_down = false;
            case Input.Buttons.RIGHT  -> mouse_buttons.right_mouse_down  = false;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
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
