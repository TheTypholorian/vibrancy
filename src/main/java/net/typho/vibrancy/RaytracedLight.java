package net.typho.vibrancy;

import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Objects;

public interface RaytracedLight extends NativeResource {
    default boolean isVisible() {
        return true;
    }

    void render(boolean raytrace);

    double lazyDistance(Vec3d vec);

    record Quad(
            Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
            Vector3f n1, Vector3f n2, Vector3f n3, Vector3f n4,
            Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4,
            Vector3f n, float d,
            Vector3f e1, Vector3f e2,
            boolean sample
    ) {
        public static final int BYTES = 40 * Float.BYTES;

        public Quad(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                    Vector3f n1, Vector3f n2, Vector3f n3, Vector3f n4,
                    Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4, boolean sample) {
            this(
                    v1, v2, v3, v4, n1, n2, n3, n4, uv1, uv2, uv3, uv4,
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize(),
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize().dot(v1),
                    new Vector3f(v2).sub(v1),
                    new Vector3f(v4).sub(v1),
                    sample
            );
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Quad quad = (Quad) o;
            return Objects.equals(v1, quad.v1) && Objects.equals(v2, quad.v2) && Objects.equals(v3, quad.v3) && Objects.equals(v4, quad.v4);
        }

        @Override
        public int hashCode() {
            return Objects.hash(v1, v2, v3, v4);
        }

        public Quad project(Quad surface, Vector3f origin, float radius) {
            Vector3f[] verts = {v1, v2, v3, v4};
            boolean[] missed = {false, false, false, false};
            Vector3f[] fars = new Vector3f[4];
            Float[] lens = {null, null, null, null};

            for (int i = 0; i < 4; i++) {
                Vector3f vert = getVert(i);
                Vector3f dir = vert.sub(origin, new Vector3f()).normalize();

                float denom = dir.dot(surface.n);

                if (denom >= 0) {
                    missed[i] = true;
                    fars[i] = dir.mul(radius, new Vector3f()).add(vert);
                } else {
                    Vector3f dir1 = verts[i].sub(origin, new Vector3f());
                    float t = (surface.d - origin.dot(surface.n)) / denom;

                    if (dir1.length() > t - 1e-3) {
                        return null;
                    }

                    lens[i] = t;
                    verts[i] = origin.add(dir.mul(t), new Vector3f());
                }
            }

            for (int i = 0; i < 4; i++) {
                if (missed[i]) {
                    Vector3f tFar = getAdj(i, lens, fars);

                    if (tFar == null) {
                        return null;
                    }

                    Vector3f mFar = fars[i];
                    Vector3f dir = mFar.sub(tFar, new Vector3f()).normalize();

                    float denom = dir.dot(surface.n);

                    if (denom >= 0) {
                        return null;
                    }

                    float t = (surface.d - mFar.dot(surface.n)) / denom;

                    verts[i] = dir.normalize(t, new Vector3f()).add(mFar);
                }
            }

            return new Quad(
                    verts[0], verts[1], verts[2], verts[3],
                    n1, n2, n3, n4, uv1, uv2, uv3, uv4, sample
            );
        }

        public <T> T getAdj(int i, Float[] lens, T[] vals) {
            return switch (i) {
                case 0 -> lens[1] == null ? vals[3] : vals[1];
                case 1 -> lens[2] == null ? vals[0] : vals[2];
                case 2 -> lens[3] == null ? vals[1] : vals[3];
                case 3 -> lens[0] == null ? vals[2] : vals[0];
                default -> throw new IllegalArgumentException(String.valueOf(i));
            };
        }

        public Vector3f getVert(int i) {
            return switch (i) {
                case 0 -> v1;
                case 1 -> v2;
                case 2 -> v3;
                case 3 -> v4;
                default -> throw new IllegalArgumentException(String.valueOf(i));
            };
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

            float d11 = e1.dot(e1);
            float d12 = e1.dot(e2);
            float d22 = e2.dot(e2);
            float invDet = 1.0f / (d11 * d22 - d12 * d12);

            float inv11 =  d22 * invDet;
            float inv12 = -d12 * invDet;
            float inv21 = -d12 * invDet;
            float inv22 =  d11 * invDet;

            buf.putFloat(inv11).putFloat(inv12).putFloat(inv21).putFloat(inv22);
        }
    }
}
