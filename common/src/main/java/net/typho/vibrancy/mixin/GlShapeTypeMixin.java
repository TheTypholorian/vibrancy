package net.typho.vibrancy.mixin;

import net.typho.big_shot_lib.api.client.opengl.util.GlIndexType;
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType;
import net.typho.big_shot_lib.api.util.buffers.BufferUploader;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Mixin(value = GlShapeType.class, remap = false)
public class GlShapeTypeMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public final void uploadIndices(int count, GlIndexType type, BufferUploader out) {
        switch (type) {
            case UBYTE -> {
                ByteBuffer buffer = MemoryUtil.memAlloc(count * type.sizeBytes).order(ByteOrder.nativeOrder());
                int x = count / 6;

                for (int i = 0, j = 0; i < x; i++, j += 4) {
                    buffer.put((byte) j);
                    buffer.put((byte) (j + 1));
                    buffer.put((byte) (j + 2));
                    buffer.put((byte) (j + 2));
                    buffer.put((byte) (j + 3));
                    buffer.put((byte) j);
                }

                out.upload(buffer.flip());
                MemoryUtil.memFree(buffer);
            }
            case USHORT -> {
                ByteBuffer buffer = MemoryUtil.memAlloc(count * type.sizeBytes).order(ByteOrder.nativeOrder());
                int x = count / 6;

                for (int i = 0, j = 0; i < x; i++, j += 4) {
                    buffer.putShort((short) j);
                    buffer.putShort((short) (j + 1));
                    buffer.putShort((short) (j + 2));
                    buffer.putShort((short) (j + 2));
                    buffer.putShort((short) (j + 3));
                    buffer.putShort((short) j);
                }

                out.upload(buffer.flip());
                MemoryUtil.memFree(buffer);
            }
            case UINT -> {
                ByteBuffer buffer = MemoryUtil.memAlloc(count * type.sizeBytes).order(ByteOrder.nativeOrder());
                int x = count / 6;

                for (int i = 0, j = 0; i < x; i++, j += 4) {
                    buffer.putInt(j);
                    buffer.putInt(j + 1);
                    buffer.putInt(j + 2);
                    buffer.putInt(j + 2);
                    buffer.putInt(j + 3);
                    buffer.putInt(j);
                }

                out.upload(buffer.flip());
                MemoryUtil.memFree(buffer);
            }
        }
    }
}
