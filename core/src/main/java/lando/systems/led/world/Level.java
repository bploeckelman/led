package lando.systems.led.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import lando.systems.led.Main;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

import static lando.systems.led.world.Level.DragHandle.Dir.*;

// TODO:
//  - add: 'handle' for moving the level
//    either a circle with diameter equal to the shortest axis
//    or a rect that covers most of the interior
//  - add: move methods
//  - add: optional layers (tile, entity, ???), maintains their own grid size
//  - fix: reorient handles if a resize inverts the bounds  (ie. right edge dragged past left edge, etc..)

public class Level {

    public static class DragHandle {
        public enum Dir { left, right, up, down }

        public final Dir dir;

        // NOTE: radius in this circle is the 'effective radius'
        //   taking into account the current camera zoom factor
        //   the 'real' radius is stored in world_radius
        public Circle circle = new Circle(0, 0, default_handle_radius);
        public float world_radius = default_handle_radius;
        public boolean hovered = false;

        public DragHandle(Dir dir) {
            this.dir = dir;
        }

        public void render(ShapeDrawer drawer) {
            drawer.setColor(hovered ? handle : handle_dim);
            drawer.filledCircle(circle.x,  circle.y, circle.radius);
            drawer.setColor(Color.WHITE);
        }
    }

    public static final int default_grid_size = 16;
    public static final int default_handle_radius = 5;
    public static final Point default_pixel_bounds = Point.at(
            10 * default_grid_size, 6 * default_grid_size);

    private static final Color outline       = new Color(0xffd700ff);
    private static final Color outline_dim   = new Color(0xaf770033);
    private static final Color highlight     = new Color(0xdaa5203f);
    private static final Color highlight_dim = new Color(0xca951033);
    private static final Color handle        = new Color(100 / 255f, 255 / 255f,  100 / 255f, 0.8f);
    private static final Color handle_dim    = new Color(150 / 255f, 150 / 255f, 150 / 255f, 0.33f);

    RectI pixel_bounds = RectI.zero();
    Matrix4 sideways_text_transform = new Matrix4();

    public final ObjectMap<DragHandle.Dir, DragHandle> drag_handles;

    public Level(Point pixel_pos) {
        this.pixel_bounds.set(pixel_pos.x, pixel_pos.y, default_pixel_bounds.x, default_pixel_bounds.y);
        this.drag_handles = new ObjectMap<>(4);
        this.drag_handles.put(left,  new DragHandle(left));
        this.drag_handles.put(right, new DragHandle(right));
        this.drag_handles.put(up,    new DragHandle(up));
        this.drag_handles.put(down,  new DragHandle(down));
        update_handles();
    }

    public void render(ShapeDrawer drawer, SpriteBatch batch, boolean is_active) {
        // interior
        drawer.filledRectangle(pixel_bounds.x, pixel_bounds.y, pixel_bounds.w, pixel_bounds.h, is_active ? highlight : highlight_dim);

        // exterior
        drawer.setColor(is_active ? outline : outline_dim);
        drawer.rectangle(pixel_bounds.x, pixel_bounds.y, pixel_bounds.w, pixel_bounds.h, 2f, JoinType.SMOOTH);
        drawer.setColor(Color.WHITE);

        if (is_active) {
            // handles
            for (var handle : drag_handles.values()) {
                handle.render(drawer);
            }

            // sizes
            Main.layout.setText(Main.font, String.format("%d px", pixel_bounds.w), Color.WHITE, pixel_bounds.w, Align.center, false);
            Main.font.draw(batch, Main.layout, pixel_bounds.x, pixel_bounds.y - 30);

            // T_T all this just for sideways text
            batch.end();
            {
                Main.layout.setText(Main.font, String.format("%d px", pixel_bounds.h), Color.WHITE, pixel_bounds.h, Align.center, false);

                var x = pixel_bounds.x - Main.layout.width - 10;
                var y = pixel_bounds.y;
                sideways_text_transform.idt()
                        .rotate(Vector3.Z, 90f)
                        .trn(x, y, 0);

                var prev_matrix = batch.getTransformMatrix().cpy();
                batch.setTransformMatrix(sideways_text_transform);
                batch.begin();
                {
                    Main.font.draw(batch, Main.layout, 0, 0);
                }
                batch.end();
                batch.setTransformMatrix(prev_matrix);
            }
            batch.begin();

        }
    }

    public void set_left_bound(float x) {
        pixel_bounds.w = pixel_bounds.w + (pixel_bounds.x - (int) x);
        pixel_bounds.x = (int) x;
        update_handles();
    }

    public void set_right_bound(float x) {
        pixel_bounds.w = (int) x - pixel_bounds.x;
        update_handles();
    }

    public void set_down_bound(float y) {
        pixel_bounds.h = pixel_bounds.h + (pixel_bounds.y - (int) y);
        pixel_bounds.y = (int) y;
        update_handles();
    }

    public void set_up_bound(float y) {
        pixel_bounds.h = (int) y - pixel_bounds.y;
        update_handles();
    }

    public void update_handles() {
        // NOTE: the radii should all be the same,
        //  they've been updated relative to camera zoom level
        var offset = drag_handles.get(left).circle.radius;
        drag_handles.get(left)  .circle.setPosition(pixel_bounds.x - offset,                  pixel_bounds.y + pixel_bounds.h / 2);
        drag_handles.get(right) .circle.setPosition(pixel_bounds.x + offset + pixel_bounds.w, pixel_bounds.y + pixel_bounds.h / 2);
        drag_handles.get(up)    .circle.setPosition(pixel_bounds.x + pixel_bounds.w / 2, pixel_bounds.y + pixel_bounds.h + offset);
        drag_handles.get(down)  .circle.setPosition(pixel_bounds.x + pixel_bounds.w / 2, pixel_bounds.y - offset);
    }

}
