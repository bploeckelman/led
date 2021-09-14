package lando.systems.led.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import lando.systems.led.input.Inputs;
import lando.systems.led.utils.RectI;
import lombok.RequiredArgsConstructor;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.lang.reflect.InvocationTargetException;

public abstract class Layer {

    public enum Type { tile, entity }

    public final Type type;
    public final Level level;
    public final ObjectMap<Class<? extends Attribute>, Attribute> attributes;

    public Data data;

    private Layer(Type type, Level level) {
        this.type = type;
        this.level = level;
        this.data = null;
        this.attributes = new ObjectMap<>();
    }

    public abstract void render(ShapeDrawer drawer, SpriteBatch batch);

    public <T extends Attribute> T get_attribute(Class<T> clazz) {
        var attrib = attributes.get(clazz);
        return clazz.cast(attrib);
    }

    public <T extends Attribute> T add_attribute(Class<T> clazz) {
        T instance = null;
        try {
            var ctor = clazz.getConstructors()[0];
            instance = clazz.cast(ctor.newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Gdx.app.error("failed", "Layer add_attribute", e);
        }
        if (instance != null) {
            attributes.put(clazz, instance);
        }
        return instance;
    }

    public String name() {
        var name_attrib = (NameAttrib) attributes.get(NameAttrib.class);
        if (name_attrib != null) {
            return name_attrib.name;
        }
        return "[no name attribute]";
    }

    // ----------------------------------------------------

    public static class Tiles extends Layer {
        private static int count = 0;

        private final Rectangle scissors = new Rectangle();
        private final Rectangle clip_bounds = new Rectangle();

        public Tiles(Level level) {
            super(Type.tile, level);
            this.attributes.put(NameAttrib.class, new NameAttrib(Tiles.class.getSimpleName() + count++));
            this.attributes.put(TilesetAttrib.class, new TilesetAttrib());
            this.attributes.put(GridAttrib.class, new GridAttrib());
            regenerate();
        }

        public void regenerate() {
            var grid_attrib = get_attribute(GridAttrib.class);
            data = new TileData(level.pixel_bounds, grid_attrib);
            clip_bounds.set(level.pixel_bounds.x, level.pixel_bounds.y, level.pixel_bounds.w, level.pixel_bounds.h);
        }

        @Override
        public void render(ShapeDrawer drawer, SpriteBatch batch) {
            var tile_data = (TileData) data;
            var grid_attrib = get_attribute(GridAttrib.class);
            var tileset_attrib = get_attribute(TilesetAttrib.class);

            // tile layers can extend slightly past a level's pixel bounds
            // update the scissor stack to clip it to pixel bounds
            if (tile_data.visible) {
                clip_bounds.set(level.pixel_bounds.x, level.pixel_bounds.y, level.pixel_bounds.w, level.pixel_bounds.h);
                ScissorStack.calculateScissors(Inputs.camera_input.get_camera(), batch.getTransformMatrix(), clip_bounds, scissors);
                if (ScissorStack.pushScissors(scissors)) {
                    for (var tile : tile_data.tiles) {
                        tile.render(drawer, batch, level, grid_attrib, tileset_attrib);
                    }
                    batch.flush();
                    ScissorStack.popScissors();
                }
            }
        }
    }

    public static class Entities extends Layer {
        private static int count = 0;

        public Entities(Level level) {
            super(Type.entity, level);
            this.data = new EntityData();
            this.attributes.put(NameAttrib.class, new NameAttrib(Entities.class.getSimpleName() + count++));
            this.attributes.put(GridAttrib.class, new GridAttrib());
        }

        @Override
        public void render(ShapeDrawer drawer, SpriteBatch batch) {
            var entity_data = (EntityData) data;
            for (var entity : entity_data.entities) {
                entity.render(drawer, batch);
            }
        }
    }

    // ----------------------------------------------------

    static abstract class Data {}

    public static class TileData extends Data {
        public boolean visible;
        public final int cols;
        public final int rows;
        public final Array<Tile> tiles;
        public TileData(RectI pixel_bounds, GridAttrib grid) {
            this.visible = true;
            this.cols = MathUtils.ceil((float) pixel_bounds.w / grid.size);
            this.rows = MathUtils.ceil((float) pixel_bounds.h / grid.size);
            this.tiles = new Array<>(cols * rows);
            for (int i = 0; i < cols * rows; i++) {
                var x = i / rows;
                var y = i % rows;
                tiles.add(new Tile(x, y));
            }
        }
    }

    public static class EntityData extends Data {
        public Array<Entity> entities = new Array<>();
    }

    // ----------------------------------------------------

    static abstract class Attribute {}

    @RequiredArgsConstructor
    public static class NameAttrib extends Attribute {
        public final String name;
    }

    @RequiredArgsConstructor
    public static class GridAttrib extends Attribute {
        public static final int default_size = 16;
        public final int size;
        public GridAttrib() {
            this.size = default_size;
        }
    }

    @RequiredArgsConstructor
    public static class TilesetAttrib extends Attribute {
        public final Tileset tileset = new Tileset();
    }

}
