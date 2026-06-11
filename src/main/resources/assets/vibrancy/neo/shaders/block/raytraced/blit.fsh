#version 430

#include "vibrancy:fragment"
#include "vibrancy:rays"

struct GridCell {
    ivec3 pos;
    uint range;
};

layout(std430) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};
layout(std430) buffer GridBuffer {
    ivec3 gridMin;
    ivec3 gridSize;
    GridCell gridCells[];
};

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;

uniform vec3 LightPos;
uniform ivec3 LightVoxel;
uniform vec3 LightColor;
uniform float LightRadius;
uniform float LightBrightness;

in vec3 vertexPos;

out vec3 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    vec3 invDir;
    float len;
};

Ray ray(vec3 pos) {
    vec3 delta = LightPos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);
    return Ray(pos, dir, 1 / dir, len);
}

vec3 test(Ray ray) {
    ivec3 gridMax = gridMin + gridSize;
    ivec3 voxel = ivec3(floor(ray.pos));
    ivec3 step = ivec3(sign(ray.dir));

    vec3 nextPos;
    nextPos.x = ray.dir.x > 0 ? float(voxel.x + 1) : float(voxel.x);
    nextPos.y = ray.dir.y > 0 ? float(voxel.y + 1) : float(voxel.y);
    nextPos.z = ray.dir.z > 0 ? float(voxel.z + 1) : float(voxel.z);

    vec3 tMax = (nextPos - ray.pos) * ray.invDir;
    vec3 tDelta = abs(ray.invDir);

    vec3 tint = vec3(0);
    float denom = 0;

    while (!(any(lessThan(voxel, gridMin)) || any(greaterThanEqual(voxel, gridMax)) || voxel == ivec3(0))) {
        for (uint i = 0; i < gridCells.length(); i++) {
            GridCell cell = gridCells[i];

            if (cell.pos == voxel) {
                uint from = cell.range >> 16;
                uint to = cell.range & 0xFFFFu;

                if (from != to) {
                    for (uint j = from; j < to; j++) {
                        float dist;
                        vec4 outColor;
                        Quad quad = shadowQuads[i];

                        if (sampleQuad(false, Sampler0, Sampler0Size, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
                            if (outColor.a == 1) {
                                return vec3(0);
                            } else if (outColor.a != 0) {
                                tint += outColor.rgb * outColor.a;
                                denom += outColor.a;
                            }
                        }
                    }
                }

                break;
            }
        }

        ivec3 oldVoxel = voxel;

        if (tMax.x < tMax.y) {
            if (tMax.x < tMax.z) {
                voxel.x += step.x;
                tMax.x += tDelta.x;
            } else {
                voxel.z += step.z;
                tMax.z += tDelta.z;
            }
        } else {
            if (tMax.y < tMax.z) {
                voxel.y += step.y;
                tMax.y += tDelta.y;
            } else {
                voxel.z += step.z;
                tMax.z += tDelta.z;
            }
        }

        if (all(equal(oldVoxel, voxel))) {
            return vec3(1);
        }
    }

    if (denom > 0) {
        return tint / denom;
    } else {
        return vec3(1);
    }
}

void main() {
    fragColor = test(ray(vertexPos));
}
