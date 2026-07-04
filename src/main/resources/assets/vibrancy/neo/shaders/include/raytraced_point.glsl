#include "vibrancy:rays"

struct RaytracedPointLight {
    vec3 pos;
    uint color;
    float radius;
    uint shadowRadius;
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
    uint shadowGrid[];
};

ivec3 getShadowGridIncrement(RaytracedPointLight light, DDAState dda) {
    uint size = light.shadowRadius * 2 + 1;
    return dda.step * ivec3(size * size, size, 1);
}

uint getShadowGridIndex(RaytracedPointLight light, ivec3 voxel) {
    ivec3 voxel1 = voxel + ivec3(light.shadowRadius);
    uint size = light.shadowRadius * 2 + 1;
    return uint((voxel1.x * size + voxel1.y) * size + voxel1.z);
}