#include "vibrancy:rays"

struct RaytracedPointLight {
    vec3 pos;
    uint color;
    float radius;
    uint shadowRadius;
    uint cellRangeStart;
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
    uint shadowGrid[];
};

vec3 sampleRaytracedPointLight(RaytracedPointLight light, vec3 fragPos) {
    vec3 delta = light.pos - fragPos;
    float distSq = dot(delta, delta);
    float radiusSq = light.radius * light.radius;

    if (distSq >= radiusSq) {
        return vec3(0);
    }

    float s = sqrt(distSq) / light.radius;
    float a = 1 - s;
    return a * a * a * unpackUnorm4x8(light.color).rgb;
}