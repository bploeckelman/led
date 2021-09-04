package lando.systems.led.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Circle;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Level {

    public static final int default_grid_size = 16;
    public static final Point default_pixel_bounds = Point.at(10 * default_grid_size, 6 * default_grid_size);
    public static final int default_handle_radius = 5;

    private static final Color outline       = new Color(0xffd700ff);
    private static final Color outline_dim   = new Color(0xaf770033);
    private static final Color highlight     = new Color(0xdaa5203f);
    private static final Color highlight_dim = new Color(0xca951033);
    private static final Color handle        = new Color(100 / 255f, 255 / 255f,  100 / 255f, 0.8f);
    private static final Color handle_dim    = new Color(150 / 255f, 150 / 255f, 150 / 255f, 0.33f);

    RectI pixel_bounds = RectI.zero();

    public Circle left_handle   = new Circle();
    public Circle right_handle  = new Circle();
    public Circle top_handle    = new Circle();
    public Circle bottom_handle = new Circle();

    public boolean left_handle_active   = false;
    public boolean right_handle_active  = false;
    public boolean top_handle_active    = false;
    public boolean bottom_handle_active = false;

    // TODO: *** everything should be pixel bounds at this scope ***
    //   each Level$Layer will have it's own grid with independent sizes

    // TODO:
    //  - add: handles for each edge (or handle that in world input only for active level?)
    //  - add: move and resize methods
    //  - add: optional layers (tile, entity, ???)

    public Level(Point pixel_pos) {
        this.pixel_bounds.set(pixel_pos.x, pixel_pos.y, default_pixel_bounds.x, default_pixel_bounds.y);

        var offset = default_handle_radius;
        this.left_handle   .set(pixel_bounds.x - offset,                  pixel_bounds.y + pixel_bounds.h / 2, default_handle_radius);
        this.right_handle  .set(pixel_bounds.x + offset + pixel_bounds.w, pixel_bounds.y + pixel_bounds.h / 2, default_handle_radius);
        this.top_handle    .set(pixel_bounds.x + pixel_bounds.w / 2, pixel_bounds.y + pixel_bounds.h + offset, default_handle_radius);
        this.bottom_handle .set(pixel_bounds.x + pixel_bounds.w / 2, pixel_bounds.y - offset, default_handle_radius);
    }

    public void render(ShapeDrawer drawer, boolean is_active) {
        // interior
        drawer.filledRectangle(pixel_bounds.x, pixel_bounds.y, pixel_bounds.w, pixel_bounds.h, is_active ? highlight : highlight_dim);

        // exterior
        drawer.setColor(is_active ? outline : outline_dim);
        drawer.rectangle(pixel_bounds.x, pixel_bounds.y, pixel_bounds.w, pixel_bounds.h, 2f, JoinType.SMOOTH);
        drawer.setColor(Color.WHITE);

        // handles
        if (is_active) {
            var radius = default_handle_radius;

            drawer.setColor(left_handle_active ? handle : handle_dim);
            drawer.filledCircle(left_handle.x,  left_handle.y,    radius);

            drawer.setColor(right_handle_active ? handle : handle_dim);
            drawer.filledCircle(right_handle.x,  right_handle.y,  radius);

            drawer.setColor(top_handle_active ? handle : handle_dim);
            drawer.filledCircle(top_handle.x,    top_handle.y,    radius);

            drawer.setColor(bottom_handle_active ? handle : handle_dim);
            drawer.filledCircle(bottom_handle.x, bottom_handle.y, radius);

            drawer.setColor(Color.WHITE);
        }
    }

}
