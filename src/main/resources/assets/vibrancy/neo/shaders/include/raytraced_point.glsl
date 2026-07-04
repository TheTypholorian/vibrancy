#include "vibrancy:rays"

const uint SHADOW_GRID_SHIFT = 1u; // TODO
const uint SHADOW_GRID_SIZE = 1u << SHADOW_GRID_SHIFT;
const uint SHADOW_GRID_SIZE_MASK = SHADOW_GRID_SIZE - 1;
const uint SHADOW_GRID_AREA = SHADOW_GRID_SIZE * SHADOW_GRID_SIZE * SHADOW_GRID_SIZE;

struct RaytracedPointLight {
    vec3 pos;
    uint color;
    float radius;
    uint shadowRadius;
    uint cellRangeStart;
};

struct ShadowGridCell {
    uint voxels[SHADOW_GRID_AREA];
};

layout(std430) readonly buffer LightBuffer {
    ivec3 worldOffset;
    uint sectionRanges[256];
    RaytracedPointLight array[];
} lights;
layout(std430) readonly buffer ShadowBuffer {
    ColoredQuad shadows[];
};
layout(std430) readonly buffer GridBuffer {
    ShadowGridCell shadowGrid[];
};

ivec3 worldPosToShadowGrid(RaytracedPointLight light, ivec3 pos) {
    ivec3 lightVoxel = ivec3(floor(light.pos));
    ivec3 relative = pos - lightVoxel;
    return (relative >> SHADOW_GRID_SHIFT) + (lightVoxel >> SHADOW_GRID_SHIFT);
}

vec3 worldPosToShadowGrid(RaytracedPointLight light, vec3 pos) {
    ivec3 lightVoxel = ivec3(floor(light.pos));
    vec3 relative = pos - lightVoxel;
    return (relative / SHADOW_GRID_SIZE) + (lightVoxel >> SHADOW_GRID_SHIFT);
}

ivec3 worldPosToShadowGridRelative(RaytracedPointLight light, ivec3 pos) {
    ivec3 lightVoxel = ivec3(floor(light.pos));
    ivec3 relative = pos - lightVoxel;
    return relative >> SHADOW_GRID_SHIFT;
}

vec3 worldPosToShadowGridRelative(RaytracedPointLight light, vec3 pos) {
    ivec3 lightVoxel = ivec3(floor(light.pos));
    vec3 relative = pos - lightVoxel;
    return relative / SHADOW_GRID_SIZE;
}

ivec3 shadowGridRelativeToWorldPos(RaytracedPointLight light, ivec3 pos) {
    ivec3 lightVoxel = ivec3(floor(light.pos));
    return lightVoxel + (pos << SHADOW_GRID_SHIFT);
}

ivec3 getShadowGridIncrement(RaytracedPointLight light, DDAState dda) {
    uint size = light.shadowRadius * 2;
    return dda.step * ivec3(size * size, size, 1);
}

uint getShadowGridIndex(RaytracedPointLight light, ivec3 voxel) {
    ivec3 voxel1 = voxel + ivec3(light.shadowRadius);
    uint size = light.shadowRadius * 2;
    return uint((voxel1.x * size + voxel1.y) * size + voxel1.z);
}

ivec3 getShadowGridCellPosFromIndex(uint index) {
    return ivec3(index >> (SHADOW_GRID_SHIFT * 2), (index >> SHADOW_GRID_SHIFT) & SHADOW_GRID_SIZE_MASK, index & SHADOW_GRID_SIZE_MASK);
}

ivec3 getShadowGridPosFromIndex(RaytracedPointLight light, uint index) {
    uint size = light.shadowRadius * 2;
    return ivec3(index / (size * size), (index / size) % size, index % size) - ivec3(light.shadowRadius);
}