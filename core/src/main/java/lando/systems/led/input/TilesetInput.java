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
import com.badlogic.gdx.utils.IntArray;
import lando.systems.led.Assets;
import lando.systems.led.utils.Point;
import lando.systems.led.utils.RectI;
import lando.systems.led.world.Layer;
import lando.systems.led.world.Tileset;
import lando.systems.led.world.World;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class TilesetInput extends InputAdapter {

    private static final int default_tile_scale = 4;
    private static final int min_tile_scale = 2;
    private static final int max_tile_scale = 100;
    private static final int sidebar_right = 200;

    public boolean visible;
    public Tileset tileset;

    public final IntArray selected_tiles = new IntArray();

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
    private final RectI select_rect     = RectI.zero();
    private final Point touch_delta     = Point.zero();
    private final Point tiles_pan       = Point.zero();
    private final Point tiles_pan_start = Point.zero();
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
    private boolean panning;
    private boolean selecting;

    public TilesetInput(World world, OrthographicCamera camera) {
        this.world = world;
        this.camera = camera;
        this.tileset = null;
        this.visible = true;
        var size = 300;
        var header_h = 50;
        this.rect.set(sidebar_right, (int) camera.viewportHeight - size, size, size);
        this.header_rect.set(rect.x, rect.y + rect.h - header_h, size, header_h);
        this.tiles_rect.set(rect.x, rect.y, rect.w, rect.h - header_rect.h);
        var handle_size = 20;
        this.resize_handle.set(rect.x + rect.w - handle_size, rect.y, handle_size, handle_size);
        this.dragging = false;
        this.resizing = false;
        this.panning = false;
        this.selecting = false;
    }

    public void update(float dt) {
        mouse_screen.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        mouse_world.set(mouse_screen);
        camera.unproject(mouse_world);

        var has_tileset = false;

        // get the tile layer tileset attribute, if one exists
        // and regenerate tiles if it's a new one
        var level = world.get_active_level();
        if (level != null) {
            var tileset_attrib = level.get_layer_attribute(Layer.Tiles.class, Layer.TilesetAttrib.class);
            if (tileset_attrib != null) {
                if (tileset != tileset_attrib.tileset) {
                    tileset = tileset_attrib.tileset;
                    regenerate_tiles();
                }
                has_tileset = true;
                visible = true;
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
                float min = sidebar_right; // if we overlap the sidebar imgui reacts to input that it shouldn't react to T_T
                rect.x = (int) MathUtils.clamp(rect.x, min, camera.viewportWidth - rect.w);
                rect.y = (int) MathUtils.clamp(rect.y, min, camera.viewportHeight - rect.h);

                // update child window regions
                header_rect.setPosition(rect.x, rect.top() - header_rect.h);
                tiles_rect.setPosition(rect.x, rect.y);
                resize_handle.setPosition(rect.right() - resize_handle.w, rect.y);

                update_tile_rects();
            }
            // handle window resize (via drag on resize handle)
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
            // handle pan for contents of tileset viewport
            else if (panning) {
                int x = (int) mouse_world.x;
                int y = (int) mouse_world.y;

                int dx = x - (int) touch_world.x;
                int dy = y - (int) touch_world.y;

                tiles_pan.set(tiles_pan_start.x + dx, tiles_pan_start.y + dy);

                update_tile_rects();
            }
            // handle dragging a selection region for tiles in the tileset viewport
            else if (selecting) {
                int x = (int) mouse_world.x;
                int y = (int) mouse_world.y;

                // determine size based on drag position
                int new_w = x - select_rect.x;
                int new_h = y - select_rect.y;

                // clamp to keep in tileset viewport
                new_w = MathUtils.clamp(select_rect.x + new_w, tiles_rect.left(), tiles_rect.right()) - select_rect.x;
                new_h = MathUtils.clamp(select_rect.y + new_h, tiles_rect.bottom(), tiles_rect.top()) - select_rect.y;

                select_rect.setSize(new_w, new_h);
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
                        drawer.rectangle(tile.x, tile.y, tile.w, tile.h, highlight, 3);
                    }
                    if (selected_tiles.contains(i)) {
                        drawer.rectangle(tile.x, tile.y, tile.w, tile.h, selected, 2);
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

        // draw select rect
        if (selecting) {
            drawer.rectangle(select_rect.x, select_rect.y, select_rect.w, select_rect.h, selected, 2);
            drawer.rectangle(select_rect.x, select_rect.y, select_rect.w, select_rect.h, Color.ORANGE, 4);
        }
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        touch_screen.set(x, y, 0);
        touch_world.set(touch_screen);
        camera.unproject(touch_world);

        if (button == Input.Buttons.RIGHT) {
            boolean had_selection = !selected_tiles.isEmpty();
            selected_tiles.clear();
            return had_selection;
        }

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
            else if (resize_handle.contains(touch_world)) {
                resizing = true;
                touch_delta.set(resize_handle.x - (int) touch_world.x, resize_handle.y - (int) touch_world.y);
                return true;
            }
            // touched in tile viewport, start a selection
            else if (tiles_rect.contains(touch_world)) {
                select_rect.set((int) touch_world.x, (int) touch_world.y, 0, 0);
                selecting = true;
                return true;
            }
        }
        else if (button == Input.Buttons.MIDDLE) {
            panning = true;
            tiles_pan_start.set(tiles_pan);
            update_tile_rects();
            return true;
        }
        else if (button == Input.Buttons.RIGHT) {
            // consume right clicks that happen in bounds also
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            dragging = false;
            resizing = false;

            if (selecting) {
                selecting = false;
                selected_tiles.clear();

                // fixup rect origin to bottom left,
                int sx = select_rect.x;
                int sy = select_rect.y;
                int sw = select_rect.w;
                int sh = select_rect.h;
                if (sw < 0) {
                    sw *= -1;
                    sx -= sw;
                }
                if (sh < 0) {
                    sh *= -1;
                    sy -= sh;
                }
                select_rect.set(sx, sy, sw, sh);

                // figure out which tiles are in the select_rect and add them to the selected tiles list
                for (int id = 0; id < tiles.size; id++) {
                    var tile = tiles.get(id);
                    if (select_rect.overlaps(tile)) {
                        selected_tiles.add(id);
                    }
                }
                select_rect.set(0, 0, 0, 0);
            }
        }
        else if (button == Input.Buttons.MIDDLE) {
            panning = false;
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
        var tile_viewport_x = tiles_pan.x + tiles_rect.left() + margin;
        var tile_viewport_y = tiles_pan.y + tiles_rect.top()  - margin;
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
