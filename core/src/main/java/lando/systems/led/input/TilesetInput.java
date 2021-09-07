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

    private final RectI window_rect;
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
        this.window_rect = RectI.of(200, (int) camera.viewportHeight - size, size, size);
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
        var header_h = 50;

        drawer.filledRectangle(window_rect.x, window_rect.y, window_rect.w, window_rect.h, background);
        drawer.filledRectangle(window_rect.x, window_rect.top() - header_h, window_rect.w, header_h, Color.DARK_GRAY);
        drawer.rectangle(window_rect.x, window_rect.y, window_rect.w, window_rect.h, outline);

        if (tileset != null) {
            // TODO: technically only the tile grid needs to be scissored,
            //   but if it's only done around the grid then the fonts get clipped out
            //   not sure how to handle this once pan/zoom is wired up for the tile grid
            clip_bounds.set(window_rect.x, window_rect.y, window_rect.w, window_rect.h);
            ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clip_bounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
                var tile_viewport_x = window_rect.left() + margin;
                var tile_viewport_y = window_rect.top() - margin;

                var line_spacing = 4;
                var prev_scale_x = font.getData().scaleX;
                var prev_scale_y = font.getData().scaleY;
                font.getData().setScale(1f);
                {
                    var left = window_rect.left() + margin;
                    var line = window_rect.top() - margin;
                    var width = window_rect.w - 2 * margin;
                    layout.setText(font, tileset.filename, Color.LIGHT_GRAY, width, Align.left, false);
                    font.draw(batch, layout, left, line);
                    line -= layout.height + line_spacing;

                    layout.setText(font, String.format("grid size: %d", tileset.grid_size), Color.LIGHT_GRAY, width, Align.left, false);
                    font.draw(batch, layout, left, line);
                    line -= layout.height + line_spacing;

                    tile_viewport_y = line - line_spacing;
                }
                font.getData().setScale(prev_scale_x, prev_scale_y);

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
