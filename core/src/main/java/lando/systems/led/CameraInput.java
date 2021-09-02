package lando.systems.led;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraInput extends InputAdapter implements GestureDetector.GestureListener {

    static final float camera_min_x = -2000;
    static final float camera_max_x =  2000;
    static final float camera_min_y = -2000;
    static final float camera_max_y =  2000;
    static final float zoom_min =  0.05f;
    static final float zoom_max =  5f;
    static final float pan_speed = 6f;
    static final float zoom_speed = 2f;
    static final float default_zoom = 1f;

    final OrthographicCamera camera;
    final Vector3 touch_pos;
    final Vector2 target_pos;
    float target_zoom;
    boolean shift_down;
    boolean middle_mouse_down;

    public final Vector2 effective_viewport;
    public boolean panning;

    public CameraInput(OrthographicCamera camera) {
        this.camera = camera;
        this.touch_pos = new Vector3();
        this.target_pos = new Vector2();
        this.target_zoom = default_zoom;
        this.effective_viewport = new Vector2();
        this.panning = false;
        this.shift_down = false;
        this.middle_mouse_down = false;
        reset_camera();
    }

    public void reset_camera() {
        camera.zoom = default_zoom;
        camera.setToOrtho(false, Config.viewport_width, Config.viewport_height);
        camera.translate(-Config.viewport_width / 2f, -Config.viewport_height / 2f);
        camera.update();

        effective_viewport.set(
                camera.viewportWidth  * camera.zoom,
                camera.viewportHeight * camera.zoom);

        target_zoom = camera.zoom;
        target_pos.set(camera.position.x, camera.position.y);

        panning = false;
        shift_down = false;
        middle_mouse_down = false;
    }

    public void update(float dt) {
        shift_down = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                   || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        target_zoom = MathUtils.clamp(target_zoom, zoom_min, zoom_max);
        camera.zoom = Interpolation.exp10Out.apply(camera.zoom, target_zoom, zoom_speed * dt);
        effective_viewport.set(
                camera.viewportWidth  * camera.zoom,
                camera.viewportHeight * camera.zoom);

        camera.position.x = Interpolation.exp5Out.apply(camera.position.x, target_pos.x, pan_speed * dt);
        camera.position.y = Interpolation.exp5Out.apply(camera.position.y, target_pos.y, pan_speed * dt);

        camera.position.x = MathUtils.clamp(camera.position.x, camera_min_x + effective_viewport.x / 2f, camera_max_x - effective_viewport.y / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y, camera_min_y + effective_viewport.x / 2f, camera_max_y - effective_viewport.y / 2f);

        camera.update();
    }

    // ------------------------------------------------------------------------
    // GestureListener implementation

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            middle_mouse_down = true;
        }
        touch_pos.set(x, y, 0);
        camera.unproject(touch_pos);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            middle_mouse_down = false;
        }
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        if (middle_mouse_down) {
            float scale = (camera.zoom < 0.6f) ? 0.3f : 1f;
            float new_x = camera.position.x - (scale * deltaX);
            float new_y = camera.position.y + (scale * deltaY);
            target_pos.set(new_x, new_y);
            panning = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        panning = false;
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        final float slow_factor = (shift_down ? 0.1f : 1f);
        target_zoom += Math.signum(initialDistance - distance) * slow_factor;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        final boolean is_close = (camera.zoom >= 0.05f && camera.zoom <= 2f);
        final float slow_factor = (is_close || shift_down ? 0.1f : 1f);
        target_zoom += Math.signum(amountY) * slow_factor;
        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

}
