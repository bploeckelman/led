package lando.systems.led.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import lando.systems.led.Assets;
import lando.systems.led.utils.RectI;
import lando.systems.led.world.Layer;
import lando.systems.led.world.Tileset;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class TilesetInput extends InputAdapter {

    public boolean visible;
    public Tileset tileset;

    final World world;
    final OrthographicCamera camera;
    final Vector3 touch_screen;
    final Vector3 touch_world;

    private final RectI rect;
    private final RectI header_rect;
    private final RectI tiles_rect;
    private final Color background = new Color(0.2f, 0.3f, 0.2f, 0.5f);
    private final Color outline = new Color(Color.SKY);
    private final Rectangle scissors = new Rectangle();
    private final Rectangle clip_bounds = new Rectangle();

    public TilesetInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.touch_screen = new Vector3();
        this.touch_world = new Vector3();
        this.tileset = null;
        this.visible = true;
        var size = 400;
        var header_h = 50;
        this.rect = RectI.of(200, (int) camera.viewportHeight - size, size, size);
        this.header_rect = RectI.of(rect.x, rect.y + rect.h - header_h, size, header_h);
        this.tiles_rect = RectI.of(rect.x, rect.y, rect.w, rect.h - header_rect.h);
    }

    public void update(float dt) {
        var has_tileset = false;

        var level = world.get_active_level();
        if (level != null) {
            var layer = level.get_layer(Layer.Tiles.class);
            if (layer != null) {
                var tileset_attrib = layer.get_attribute(Layer.TilesetAttrib.class);
                if (tileset_attrib != null) {
                    tileset = tileset_attrib.tileset;
                    has_tileset = true;
                    visible = true;
                }
            }
        }

        if (!has_tileset) {
            tileset = null;
            visible = false;
        }
    }

    public void render_gui(ShapeDrawer drawer, SpriteBatch batch) {
        if (!visible) return;

        var font = Assets.font;
        var layout = Assets.layout;
        var margin = 8;

        // draw backgrounds
        {
            // header
            drawer.filledRectangle(header_rect.x, header_rect.top() - header_rect.h, header_rect.w, header_rect.h, Color.DARK_GRAY);
            drawer.rectangle(header_rect.x, header_rect.y, header_rect.w, header_rect.h, outline);

            // tiles
            drawer.filledRectangle(tiles_rect.x, tiles_rect.y, tiles_rect.w, tiles_rect.h, background);
            drawer.rectangle(tiles_rect.x, tiles_rect.y, tiles_rect.w, tiles_rect.h, outline);
        }

        if (tileset != null) {
            // draw header
            var line_spacing = 4;
            var prev_scale_x = font.getData().scaleX;
            var prev_scale_y = font.getData().scaleY;
            font.getData().setScale(1f);
            {
                var left = header_rect.left() + margin;
                var line = header_rect.top() - margin;
                var width = header_rect.w - 2 * margin;
                layout.setText(font, tileset.filename, Color.LIGHT_GRAY, width, Align.left, false);
                font.draw(batch, layout, left, line);
                line -= layout.height + line_spacing;

                layout.setText(font, String.format("grid size: %d", tileset.grid_size), Color.LIGHT_GRAY, width, Align.left, false);
                font.draw(batch, layout, left, line);
                line -= layout.height + line_spacing;
            }
            font.getData().setScale(prev_scale_x, prev_scale_y);
            batch.flush();

            // draw tiles viewport
            var tile_viewport_x = tiles_rect.left() + margin;
            var tile_viewport_y = tiles_rect.top() - margin;
            clip_bounds.set(tiles_rect.x, tiles_rect.y, tiles_rect.w, tiles_rect.h);
            ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clip_bounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
                var scale = 3;
                var grid = tileset.grid_size;
                var size = grid * scale;
                var tile_spacing = 4;
                for (int y = 0; y < tileset.rows; y++) {
                    for (int x = 0; x < tileset.cols; x++) {
                        var tile = tileset.get(x, y);
                        var tx = tile_viewport_x + x * size + x * tile_spacing;
                        var ty = tile_viewport_y - (y + 1) * (size + tile_spacing);
                        batch.draw(tile, tx, ty, size, size);
                    }
                }

                batch.flush();
                ScissorStack.popScissors();
            }
        }
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        touch_screen.set(x, y, 0);
        touch_world.set(touch_screen);
        camera.unproject(touch_world);

        // ...

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

}
