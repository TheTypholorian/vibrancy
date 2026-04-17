#version 430

#include "vibrancy:include/fragment.glsl"
#include "vibrancy:include/rays.glsl"

struct BVH {
    vec3 min;
    uint start;
    vec3 max;
    uint end;
};

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};
layout(std430, binding = 1) buffer BVHBuffer {
    BVH boundingVolumes[];
};

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;

uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform float LightBrightness;

in vec3 vertexPos;

out vec3 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    float len;
};

Ray ray(vec3 pos) {
    vec3 delta = LightPos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);
    return Ray(pos, dir, len);
}

void main() {
    //vec2 step = 1 / (vec2(sprite.width, sprite.height) * 3);

    Ray ray = ray(vertexPos);

    fragColor = vec3(1);
    vec3 tint = vec3(0);
    float denom = 0;

    for (uint i = 0u; i < boundingVolumes.length(); i++) {
        BVH bvh = boundingVolumes[i];

        if (raycastAABB(ray.pos, ray.dir, ray.len, AABB(bvh.min, bvh.max))) {
            for (uint j = bvh.start; j < bvh.end; j++) {
                float dist;
                vec4 outColor;
                Quad quad = shadowQuads[j];

                if (sampleQuad(Sampler0, Sampler0Size, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
                    if (outColor.a == 1) {
                        fragColor = vec3(0);
                        break;
                    } else if (outColor.a != 0) {
                        tint += outColor.rgb * outColor.a;
                        denom += outColor.a;
                    }
                }
            }
        }
    }

    if (denom > 0) {
        fragColor *= tint / denom;
    }
}
