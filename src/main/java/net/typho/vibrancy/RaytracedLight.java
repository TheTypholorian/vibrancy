package net.typho.vibrancy;

import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;

public interface RaytracedLight extends NativeResource {
    default boolean isVisible() {
        return true;
    }

    void prepare(LightRenderer renderer, CullFrustum frustum);

    void render(LightRenderer renderer);

    record Quad(
            Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
            Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4,
            Vector3f n, float d,
            Vector3f e1, Vector3f e2,
            boolean sample
    ) {
        public static final int BYTES = 36 * Float.BYTES;

        public Quad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                    Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4, boolean sample) {
            this(
                    v1, v2, v3, v4, uv1, uv2, uv3, uv4,
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize(),
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize().dot(v1),
                    new Vector3f(v2).sub(v1),
                    new Vector3f(v4).sub(v1),
                    sample
            );
        }

        public boolean raycast(Vector3f origin, Vector3f dir, float len) {
            float denom = dir.dot(n);
            if (denom >= 0.0) return false;

            float tt = (d - origin.dot(n)) / denom;
            if (tt < 1e-3 || tt > len - 1e-3) return false;

            Vector3f p = dir.mul(tt, new Vector3f()).add(origin);
            Vector3f vp = p.sub(v1, new Vector3f());

            float d11 = e1.dot(e1);
            float d12 = e1.dot(e2);
            float d22 = e2.dot(e2);
            float d1p = e1.dot(vp);
            float d2p = e2.dot(vp);

            float invDenom = 1f / (d11 * d22 - d12 * d12);
            float a = (d22 * d1p - d12 * d2p) * invDenom;
            float b = (d11 * d2p - d12 * d1p) * invDenom;

            return !(a < 0.0) && !(b < 0.0) && !(a > 1.0) && !(b > 1.0);
        }

        public void put(ByteBuffer buf) {
            buf.putFloat(v1.x).putFloat(v1.y).putFloat(v1.z).putInt(sample ? 1 : 0);
            buf.putFloat(v2.x).putFloat(v2.y).putFloat(v2.z).putFloat(0);
            buf.putFloat(v3.x).putFloat(v3.y).putFloat(v3.z).putFloat(0);
            buf.putFloat(v4.x).putFloat(v4.y).putFloat(v4.z).putFloat(0);

            buf.putFloat(uv1.x).putFloat(uv1.y);
            buf.putFloat(uv2.x).putFloat(uv2.y);
            buf.putFloat(uv3.x).putFloat(uv3.y);
            buf.putFloat(uv4.x).putFloat(uv4.y);

            buf.putFloat(n.x).putFloat(n.y).putFloat(n.z).putFloat(d);
            buf.putFloat(e1.x).putFloat(e1.y).putFloat(e1.z).putFloat(e1.dot(e1));
            buf.putFloat(e2.x).putFloat(e2.y).putFloat(e2.z).putFloat(e2.dot(e2));
        }
    }
}
