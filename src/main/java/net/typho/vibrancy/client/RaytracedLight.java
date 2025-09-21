package net.typho.vibrancy.client;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public interface RaytracedLight {
    default boolean shouldRaytrace() {
        return true;
    }

    void uploadQuads();

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
