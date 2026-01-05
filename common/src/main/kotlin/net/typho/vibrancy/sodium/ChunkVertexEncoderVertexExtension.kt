package net.typho.vibrancy.sodium;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Developers of Veil
 */
@ApiStatus.Internal
interface ChunkVertexEncoderVertexExtension {
    fun `vibrancy$getPackedNormal`(): Int

    fun `vibrancy$setNormal`(packedNormal: Int)
}
