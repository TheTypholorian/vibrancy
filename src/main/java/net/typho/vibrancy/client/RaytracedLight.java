package net.typho.vibrancy.client;

import net.minecraft.util.math.Vec3d;

import java.nio.ByteBuffer;

public interface RaytracedLight {
    default boolean shouldRaytrace() {
        return true;
    }

    void uploadQuads();

    record Quad(Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4) {
        public void put(ByteBuffer buf) {
            buf.putFloat((float) v1.x).putFloat((float) v1.y).putFloat((float) v1.z);
            buf.putFloat((float) v2.x).putFloat((float) v2.y).putFloat((float) v2.z);
            buf.putFloat((float) v3.x).putFloat((float) v3.y).putFloat((float) v3.z);
            buf.putFloat((float) v4.x).putFloat((float) v4.y).putFloat((float) v4.z);
        }
    }
}
