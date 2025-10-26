package net.typho.vibrancy;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AbstractPointLight {
    protected final Vector3d position = new Vector3d();
    protected final Vector3f color = new Vector3f(1);
    protected float radius = 1, brightness = 1;
    private boolean dirty = true;

    public void markDirty() {
        dirty = true;
    }

    public void clean() {
        dirty = false;
    }

    public Vector3d getPosition() {
        return position;
    }

    public float getRadius() {
        return radius;
    }

    public Vector3fc getColor() {
        return color;
    }

    public int getColorInt() {
        int red = (int) (color.x() / 255.0F) & 0xFF;
        int green = (int) (color.y() / 255.0F) & 0xFF;
        int blue = (int) (color.z() / 255.0F) & 0xFF;
        return red << 16 | green << 8 | blue;
    }

    public AbstractPointLight setColor(Vector3fc color) {
        return setColor(color.x(), color.y(), color.z());
    }

    public AbstractPointLight setColor(float red, float green, float blue) {
        this.color.set(red, green, blue);
        markDirty();
        return this;
    }

    public AbstractPointLight setColor(int color) {
        this.color.set(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        markDirty();
        return this;
    }

    public AbstractPointLight setBrightness(float brightness) {
        this.brightness = brightness;
        markDirty();
        return this;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public AbstractPointLight setPosition(Vector3dc position) {
        this.position.set(position);
        markDirty();
        return this;
    }

    public AbstractPointLight setPosition(double x, double y, double z) {
        this.position.set(x, y, z);
        markDirty();
        return this;
    }

    public AbstractPointLight setRadius(float radius) {
        this.radius = radius;
        markDirty();
        return this;
    }
}
