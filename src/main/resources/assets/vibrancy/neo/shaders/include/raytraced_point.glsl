#include "vibrancy:rays"

#extension GL_EXT_shader_8bit_storage : require

struct RaytracedPointLight {
    vec3 pos;
    float radius;

    uint color;
    float brightness;

    uint shadowRadius;
    uint shadowRangeStart;
    uint shadowRangeLightLength;
    uint shadowRangeLength;
    uint cellRangeStart;
};

layout(std430, binding = 0) readonly buffer u_Lights {
    ivec3 worldOffset;
    uint sectionRanges[256];
    RaytracedPointLight array[];
} lights;
layout(std430, binding = 1) readonly buffer u_Shadows {
    ColoredQuad shadows[];
};
layout(std430, binding = 2) readonly buffer u_Grids {
    uint8_t shadowGrid[];
};

ivec3 getShadowGridIncrement(uint size, DDAState dda) {
    return dda.step * ivec3(size * size, size, 1);
}

uint getShadowGridIndex(uint size, ivec3 voxel) {
    return uint((voxel.x * size + voxel.y) * size + voxel.z);
}