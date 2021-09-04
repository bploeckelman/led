package lando.systems.led.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

import static lando.systems.led.world.Level.DragHandle.Dir.*;

// TODO:
//  - fix: reorient bounds/handles if a resize inverts the bounds  (ie. right edge dragged past left edge, etc..)
//         width and height should always be positive
//  - add: optional layers (tile, entity, ???), maintains their own grid size

public class Level {

    public class DragHandle {
        public enum Dir { left, right, up, down, center }

        public final Dir dir;

        // NOTE: radius in this circle is the 'effective radius'
        //   taking into account the current camera zoom factor
        //   the 'real' radius is stored in world_radius
        public Circle circle = new Circle(0, 0, default_handle_radius);
        public Color color = handle.cpy();
        public Color color_dim = handle_dim.cpy();
        public float world_radius = default_handle_radius;
        public boolean hovered = false;

        public DragHandle(Dir dir) {
            this.dir = dir;
        }

        public void render(ShapeDrawer drawer) {
            drawer.setColor(hovered ? color : color_dim);
            if (dir == center) {
                float w = Level.this.pixel_bounds.w;
                float h = Level.this.pixel_bounds.h;
                drawer.filledRectangle(
                        circle.x - w / 2,
                        circle.y - h / 2,
                        w, h);
            } else {
                drawer.filledCircle(circle.x,  circle.y, circle.radius);
            }
            drawer.setColor(Color.WHITE);
        }
    }

    public static BitmapFont font = null;

    public static final GlyphLayout layout = new GlyphLayout();
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

    public final RectI pixel_bounds = RectI.zero();
    Matrix4 sideways_text_transform = new Matrix4();

    public final ObjectMap<DragHandle.Dir, DragHandle> drag_handles;

    public Level(Point pixel_pos) {
        this.pixel_bounds.set(pixel_pos.x, pixel_pos.y, default_pixel_bounds.x, default_pixel_bounds.y);
        this.drag_handles = new ObjectMap<>(5);
        this.drag_handles.put(left,   new DragHandle(left));
        this.drag_handles.put(right,  new DragHandle(right));
        this.drag_handles.put(up,     new DragHandle(up));
        this.drag_handles.put(down,   new DragHandle(down));
        this.drag_handles.put(center, new DragHandle(center));

        if (Level.font == null) {
            var fontFile = Gdx.files.internal("dogicapixel.ttf");
            var generator = new FreeTypeFontGenerator(fontFile);
            var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 16;
            parameter.mono = true;
            parameter.color = Color.LIGHT_GRAY;
            parameter.borderColor = new Color(0.1f, 0.1f, 0.1f, 1f);
            parameter.shadowColor = Color.BLACK;
            parameter.borderWidth = 1;
            parameter.shadowOffsetX = 1;
            parameter.shadowOffsetY = 1;
            Level.font = generator.generateFont(parameter);
            generator.dispose();
        }

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
            layout.setText(font, String.format("%d px", pixel_bounds.w), Color.WHITE, pixel_bounds.w, Align.center, false);
            font.draw(batch, layout, pixel_bounds.x, pixel_bounds.y - 30);

            // T_T all this just for sideways text
            batch.end();
            {
                layout.setText(font, String.format("%d px", pixel_bounds.h), Color.WHITE, pixel_bounds.h, Align.center, false);

                var x = pixel_bounds.x - layout.height - 30;
                var y = pixel_bounds.y;
                sideways_text_transform.idt()
                        .rotate(Vector3.Z, 90f)
                        .trn(x, y, 0);

                var prev_matrix = batch.getTransformMatrix().cpy();
                batch.setTransformMatrix(sideways_text_transform);
                batch.begin();
                {
                    font.draw(batch, layout, 0, 0);
                }
                batch.end();
                batch.setTransformMatrix(prev_matrix);
            }
            batch.begin();

        }
    }

    public void set_center_bound(float x, float y) {
        pixel_bounds.x = (int) x;
        pixel_bounds.y = (int) y;
        update_handles();
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
        drag_handles.get(left)  .circle.setPosition(pixel_bounds.x - offset,                  pixel_bounds.y + pixel_bounds.h / 2f);
        drag_handles.get(right) .circle.setPosition(pixel_bounds.x + offset + pixel_bounds.w, pixel_bounds.y + pixel_bounds.h / 2f);
        drag_handles.get(up)    .circle.setPosition(pixel_bounds.x + pixel_bounds.w / 2f, pixel_bounds.y + pixel_bounds.h + offset);
        drag_handles.get(down)  .circle.setPosition(pixel_bounds.x + pixel_bounds.w / 2f, pixel_bounds.y - offset);
        drag_handles.get(center).circle.set(
                pixel_bounds.x + pixel_bounds.w / 2f,
                pixel_bounds.y + pixel_bounds.h / 2f,
                Math.min(pixel_bounds.w / 2f, pixel_bounds.h / 2f));
        drag_handles.get(center).color.set(1f, 1f, 0f, 0.1f);
        drag_handles.get(center).color_dim.set(0.1f, 0.1f, 0.1f, 0.1f);
    }

}
