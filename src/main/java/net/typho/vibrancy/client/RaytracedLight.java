package net.typho.vibrancy.client;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.nio.ByteBuffer;
import java.util.List;

public interface RaytracedLight {
    default boolean shouldRaytrace() {
        return true;
    }

    void uploadQuads();

    record QuadGroup(Vector3i pos, List<Quad> quads) {
        public static final int MAX_QUADS = 64, SIZE = 8, BYTES = (4 + MAX_QUADS * 16) * Float.BYTES + Integer.BYTES;

        public void put(ByteBuffer buf) {
            buf.putInt(pos.x).putInt(pos.y).putInt(pos.z).putInt(0);
            buf.putInt(pos.x + SIZE).putInt(pos.y + SIZE).putInt(pos.z + SIZE).putInt(0);

            for (Quad quad : quads) {
                quad.put(buf);
            }

            buf.position(Float.BYTES * (4 + MAX_QUADS * 16));
            buf.putInt(quads.size());
        }
    }

    record Quad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
        public void put(ByteBuffer buf) {
            buf.putFloat(v1.x).putFloat(v1.y).putFloat(v1.z).putFloat(0);
            buf.putFloat(v2.x).putFloat(v2.y).putFloat(v2.z).putFloat(0);
            buf.putFloat(v3.x).putFloat(v3.y).putFloat(v3.z).putFloat(0);
            buf.putFloat(v4.x).putFloat(v4.y).putFloat(v4.z).putFloat(0);
        }

        @Override
        public @NotNull String toString() {
            return "(" + v1.x + " " + v1.y + " " + v1.z + ") (" + v2.x + " " + v2.y + " " + v2.z + ") (" + v3.x + " " + v3.y + " " + v3.z + ") (" + v4.x + " " + v4.y + " " + v4.z + ")";
        }
    }
}
