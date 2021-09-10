package lando.systems.led.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import lando.systems.led.Assets;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import lando.systems.led.world.Layer;
import lando.systems.led.world.Tileset;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class TilesetInput extends InputAdapter {

    private static final int default_tile_scale = 4;
    private static final int min_tile_scale = 1;
    private static final int max_tile_scale = 100;

    public boolean visible;
    public Tileset tileset;
    public final IntSet selected_tiles = new IntSet();

    final World world;
    final OrthographicCamera camera;
    final Vector3 touch_screen = new Vector3();
    final Vector3 touch_world = new Vector3();
    final Vector3 mouse_screen = new Vector3();
    final Vector3 mouse_world = new Vector3();

    private final RectI rect            = RectI.zero();
    private final RectI header_rect     = RectI.zero();
    private final RectI tiles_rect      = RectI.zero();
    private final RectI resize_handle   = RectI.zero();
    private final Point touch_delta     = Point.zero();
    private final Color background      = new Color(0.2f, 0.3f, 0.2f, 0.5f);
    private final Color outline         = new Color(Color.SKY);
    private final Color handle          = new Color(0x97defbff);
    private final Color handle_dim      = new Color(0x87ceebcc);
    private final Color highlight       = new Color(0x32cd32dd);
    private final Color selected        = new Color(0xff341cdd);
    private final Rectangle scissors    = new Rectangle();
    private final Rectangle clip_bounds = new Rectangle();
    private final Array<RectI> tiles = new Array<>();
    private int tile_scale = default_tile_scale;
    private boolean dragging;
    private boolean resizing;

    public TilesetInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.tileset = null;
        this.visible = true;
        var size = 300;
        var header_h = 50;
        this.rect.set(0, (int) camera.viewportHeight - size, size, size);
        this.header_rect.set(rect.x, rect.y + rect.h - header_h, size, header_h);
        this.tiles_rect.set(rect.x, rect.y, rect.w, rect.h - header_rect.h);
        var handle_size = 20;
        this.resize_handle.set(rect.x + rect.w - handle_size, rect.y, handle_size, handle_size);
        this.dragging = false;
        this.resizing = false;
    }

    public void update(float dt) {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera.unproject(mouse_world);

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
                    regenerate_tiles();
                }
            }
        }

        if (!has_tileset) {
            tileset = null;
            visible = false;
            regenerate_tiles();
        }

        if (visible) {
            // handle window drag
            if (dragging) {
                // reposition window relative to where we touched
                int x = (int) mouse_world.x;
                int y = (int) mouse_world.y;
                rect.setPosition(x + touch_delta.x, y + touch_delta.y);

                // keep window on screen
                rect.x = (int) MathUtils.clamp(rect.x, 0, camera.viewportWidth - rect.w);
                rect.y = (int) MathUtils.clamp(rect.y, 0, camera.viewportHeight - rect.h);

                // update child window regions
                header_rect.setPosition(rect.x, rect.top() - header_rect.h);
                tiles_rect.setPosition(rect.x, rect.y);
                resize_handle.setPosition(rect.right() - resize_handle.w, rect.y);

                update_tile_rects();
            }
            else if (resizing) {
                int x = (int) mouse_world.x;
                int y = (int) mouse_world.y;

                // calculate new size (and new y pos since y is bottom of rect)
                int new_x = rect.x;
                int new_y = y;
                int new_w = x - new_x;
                int new_h = rect.top() - new_y;

                // clamp to some minimum size
                int min_w = 4 * resize_handle.w;
                int min_h = 4 * resize_handle.h;
                if (new_w < min_w) {
                    new_w = min_w;
                }
                if (new_h < min_h) {
                    new_h = min_h;
                    new_y = rect.top() - new_h;
                }

                // clamp to keep on screen
                if (new_x + new_w > camera.viewportWidth) {
                    new_w = (int) camera.viewportWidth - new_x;
                }
                if (new_y < 0) {
                    new_y = 0;
                    new_h = rect.top();
                }

                // resize window
                rect.set(new_x, new_y, new_w, new_h);

                // update child window regions
                header_rect.setSize(rect.w, header_rect.h);
                tiles_rect.set(rect.x, rect.y, rect.w, rect.h - header_rect.h);
                resize_handle.setPosition(rect.right() - resize_handle.w, rect.y);
            }
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

            batch.flush();
        }

        if (tileset != null) {
            // draw header
            clip_bounds.set(header_rect.x, header_rect.y, header_rect.w - 1, header_rect.h);
            ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clip_bounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
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
                ScissorStack.popScissors();
            }

            // draw tiles viewport
            clip_bounds.set(tiles_rect.x, tiles_rect.y, tiles_rect.w, tiles_rect.h);
            ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clip_bounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
                for (int i = 0; i < tiles.size; i++) {
                    var texture = tileset.get(i);
                    var tile = tiles.get(i);
                    batch.draw(texture, tile.x, tile.y, tile.w, tile.h);
                    if (tile.contains(mouse_world)) {
                        drawer.rectangle(tile.x, tile.y, tile.w, tile.h, highlight, 4);
                    }
                    if (selected_tiles.contains(i)) {
                        drawer.rectangle(tile.x, tile.y, tile.w, tile.h, selected, 6);
                    }
                }

                batch.flush();
                ScissorStack.popScissors();
            }
        }

        {
            // draw resize handle
            var handle_color = resize_handle.contains(mouse_world) ? handle : handle_dim;
            drawer.filledRectangle(resize_handle.x, resize_handle.y, resize_handle.w, resize_handle.h, handle_color);
            drawer.rectangle(resize_handle.x, resize_handle.y, resize_handle.w, resize_handle.h, outline);
        }
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        touch_screen.set(x, y, 0);
        touch_world.set(touch_screen);
        camera.unproject(touch_world);

        // throw away the touch if it's out of bounds
        if (!rect.contains(touch_world.x, touch_world.y)) {
            return false;
        }

        if (button == Input.Buttons.LEFT) {
            // touched in header, start a window drag operation
            if (header_rect.contains(touch_world)) {
                dragging = true;
                touch_delta.set(rect.x - (int) touch_world.x, rect.y - (int) touch_world.y);
                return true;
            }
            // touched in resize handle, start a window resize operation
            if (resize_handle.contains(touch_world)) {
                resizing = true;
                touch_delta.set(resize_handle.x - (int) touch_world.x, resize_handle.y - (int) touch_world.y);
                return true;
            }
            // touched a tile, set it as active
            for (int i = 0; i < tiles.size; i++) {
                var tile_rect = tiles.get(i);
                if (tile_rect.contains(touch_world)) {
                    selected_tiles.add(i);
                    return true;
                }
            }

            // otherwise if we touched in the tile viewport bounds, that shouldn't pass to the next input processor
            if (tiles_rect.contains(touch_world)) {
                return true;
            }
        }
        else if (button == Input.Buttons.MIDDLE) {
            tile_scale = default_tile_scale;
            update_tile_rects();
            // TODO: if touched in tile rect, initiate a pan for just the tile rect contents
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            dragging = false;
            resizing = false;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (tiles_rect.contains(mouse_world)) {
            // TODO: what axis to use, take amount into account or just adjust per call?
            tile_scale -= Math.signum(amountY);
            tile_scale = MathUtils.clamp(tile_scale, min_tile_scale, max_tile_scale);
            update_tile_rects();
            return true;
        }
        return false;
    }

    private void regenerate_tiles() {
        tiles.clear();
        if (tileset == null) return;

        int num_tiles = tileset.rows * tileset.cols;
        for (int i = 0; i < num_tiles; i++) {
            tiles.add(RectI.zero());
        }

        update_tile_rects();
    }

    private void update_tile_rects() {
        if (tileset == null) return;

        var margin = 5;
        var tile_spacing = 4;
        var tile_viewport_x = tiles_rect.left() + margin;
        var tile_viewport_y = tiles_rect.top()  - margin;
        var size = tileset.grid_size * tile_scale;

        for (int y = 0, tile_id = 0; y < tileset.rows; y++) {
            for (int x = 0; x < tileset.cols; x++) {
                var tx = tile_viewport_x + x * size + x * tile_spacing;
                var ty = tile_viewport_y - (y + 1) * (size + tile_spacing);
                tiles.get(tile_id).set(tx, ty, size, size);
                tile_id++;
            }
        }
    }

}
