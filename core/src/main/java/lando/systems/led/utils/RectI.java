package lando.systems.led.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class RectI implements Pool.Poolable {

    public static Pool<RectI> pool = Pools.get(RectI.class);

    public int x;
    public int y;
    public int w;
    public int h;

    public RectI() {}

    private RectI(RectI other) {
        set(other);
    }

    private RectI(int x, int y, int w, int h) {
        set(x, y, w, h);
    }

    public static RectI zero() {
        return new RectI(0, 0, 0, 0);
    }

    public static RectI of(RectI other) {
        return new RectI(other);
    }

    public static RectI of(int x, int y, int w, int h) {
        return new RectI(x, y, w, h);
    }

    public RectI set(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public RectI set(RectI other) {
        return set(other.x, other.y, other.w, other.h);
    }

    public RectI setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public RectI setSize(int w, int h) {
        this.w = w;
        this.h = h;
        return this;
    }

    public int left()   { return x; }
    public int right()  { return x + w; }
    public int bottom() { return y; }
    public int top()    { return y + h; }

    @Override
    public void reset() {
        x = 0;
        y = 0;
        w = 0;
        h = 0;
    }

    public boolean contains(int x, int y) {
        return (x >= this.x && x <= this.x + this.w
             && y >= this.y && y <= this.y + this.h);
    }

    public boolean contains(Point point) {
        return contains(point.x, point.y);
    }

    public boolean contains(float x, float y) {
        return contains((int) x, (int) y);
    }

    public boolean contains(Vector2 point) {
        return contains(point.x, point.y);
    }

    public boolean contains(Vector3 point) {
        return contains(point.x, point.y);
    }

    public boolean contains(RectI other) {
        return left()   <= other.left()
            && bottom() <= other.bottom()
            && right()  >= other.right()
            && top()    >= other.top();
    }

    public boolean overlaps(RectI other) {
        return left()   < other.right()
            && bottom() < other.top()
            && right()  > other.left()
            && top()    > other.bottom();
    }

}
