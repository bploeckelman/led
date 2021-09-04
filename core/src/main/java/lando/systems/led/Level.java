package lando.systems.led;

import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;

public class Level {

    public static final int default_grid_size = 16;
    public static final Point default_pixel_bounds = Point.at(10 * default_grid_size, 6 * default_grid_size);

    RectI pixel_bounds = RectI.zero();

    // TODO: *** everything should be pixel bounds at this scope ***
    //   each Level$Layer will have it's own grid with independent sizes

    // TODO:
    //  - add: handles for each edge (or handle that in world input only for active level?)
    //  - add: move and resize methods
    //  - add: optional layers (tile, entity, ???)

    public Level(Point pixel_pos) {
        this.pixel_bounds.set(pixel_pos.x, pixel_pos.y, default_pixel_bounds.x, default_pixel_bounds.y);
    }

}
