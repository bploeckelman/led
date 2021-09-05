package lando.systems.led.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import lando.systems.led.utils.Point;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Tile {

    public final Point grid = Point.zero();
    public int tileset_index = -1;

    public Tile(int x, int y) {
        this.grid.set(x, y);
    }

    private static final Color color = new Color(1f, 160f / 255f, 0f, 0.5f);
    private static final Color outline = new Color(139f / 255f, 69f / 255f, 19f / 255f, 0.8f);

    public void render(ShapeDrawer drawer, SpriteBatch batch, Level level, Layer.GridAttrib grid_attrib, Layer.TilesetAttrib tileset_attrib) {
        var grid_size = grid_attrib.size;
        var origin = Point.pool.obtain().set(level.pixel_bounds.x, level.pixel_bounds.y);

        drawer.filledRectangle(origin.x + grid.x * grid_size, origin.y + grid.y * grid_size, grid_size, grid_size, color);
        drawer.rectangle(origin.x + grid.x * grid_size, origin.y + grid.y * grid_size, grid_size, grid_size, outline);

        var texture = tileset_attrib.tileset.get(tileset_index);
        if (texture != null) {
            batch.draw(texture, origin.x + grid.x * grid_size, origin.y + grid.y * grid_size, grid_size, grid_size);
        }

        Point.pool.free(origin);
    }

}
