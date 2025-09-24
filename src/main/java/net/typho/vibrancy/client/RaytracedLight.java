package net.typho.vibrancy.client;

import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

public interface RaytracedLight extends NativeResource {
    default boolean isVisible() {
        return true;
    }

    void prepare(LightRenderer renderer, CullFrustum frustum);

    void render(LightRenderer renderer);

    record Quad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4) implements Iterable<Vector3f> {
        public static final int BYTES = 24 * Float.BYTES;

        public void put(ByteBuffer buf) {
            buf.putFloat(v1.x).putFloat(v1.y).putFloat(v1.z).putFloat(0);
            buf.putFloat(v2.x).putFloat(v2.y).putFloat(v2.z).putFloat(0);
            buf.putFloat(v3.x).putFloat(v3.y).putFloat(v3.z).putFloat(0);
            buf.putFloat(v4.x).putFloat(v4.y).putFloat(v4.z).putFloat(0);
            buf.putFloat(uv1.x).putFloat(uv1.y);
            buf.putFloat(uv2.x).putFloat(uv2.y);
            buf.putFloat(uv3.x).putFloat(uv3.y);
            buf.putFloat(uv4.x).putFloat(uv4.y);
        }

        public Vector3f center() {
            return new Vector3f(v1).add(v2).add(v3).add(v4).div(4);
        }

        @Override
        public @NotNull String toString() {
            return "(" + v1.x + " " + v1.y + " " + v1.z + ") (" + v2.x + " " + v2.y + " " + v2.z + ") (" + v3.x + " " + v3.y + " " + v3.z + ") (" + v4.x + " " + v4.y + " " + v4.z + ")";
        }

        @Override
        public @NotNull Iterator<Vector3f> iterator() {
            return Arrays.stream(new Vector3f[]{v1, v2, v3, v4}).iterator();
        }
    }
}
