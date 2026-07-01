#version 430 core

#include "sodium:globals"
#include "sodium:fog"
#include "vibrancy:rays"
#include "vibrancy:fragment"

in vec3 v_Pos;
in flat uint v_SectionPos;
in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_FragDistance; // The fragment's distance from the camera (cylindrical and spherical)

uniform sampler2D u_BlockTex; // The block texture
uniform sampler2D u_ReflectionTex; // The reflection texture
uniform sampler2D u_TransmissionTex; // The transmission texture

out vec4 fragColor; // The output fragment for the color framebuffer

struct Light {
    vec3 pos;
    uint color;
    uint radius;
    uint shadowRadius;
    uint cellRangeStart;
};
struct ShadowCell {
    uint shadowIndex;
    uint data;
};

layout(std430, binding = 0) readonly buffer LightBuffer {
    ivec3 worldOffset;
    uint sectionRanges[256];
    Light array[];
} lights;
layout(std430, binding = 1) readonly buffer ShadowBuffer {
    ColoredQuad shadows[];
};
layout(std430, binding = 2) readonly buffer GridBuffer {
    ShadowCell shadowGrid[];
};

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(source, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

// Rotated Grid Super-Sampling
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColor = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColor += textureLod(source, sampleUV, mipLevelExact);
    }
    rgssColor *= 0.25;

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

struct Ray {
    vec3 pos;
    vec3 dir;
    vec3 invDir;
    float len;
};

Ray ray(Light light, vec3 pos) {
    vec3 delta = light.pos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);
    return Ray(pos, dir, 1 / dir, len);
}

bool isInGrid(ivec3 voxel, ivec3 gridMin, ivec3 gridMax) {
    return any(greaterThanEqual(voxel, gridMin)) && any(lessThan(voxel, gridMax)) && voxel != ivec3(0);
}

uint getGridIndex(ivec3 voxel, uint size) {
    ivec3 voxel1 = voxel + ivec3(size);
    return uint((voxel1.x * size + voxel1.y) * size + voxel1.z);
}

vec3 test(Ray ray, ivec3 lightPos, uint radius, uint cellRangeStart) {
    ivec3 voxel = ivec3(floor(ray.pos) - lightPos);
    ivec3 step = ivec3(sign(ray.dir));
    ivec3 indexStep = step * ivec3(radius * radius, radius, 1);

    vec3 nextPos;
    nextPos.x = ray.dir.x > 0 ? float(voxel.x + 1) : float(voxel.x);
    nextPos.y = ray.dir.y > 0 ? float(voxel.y + 1) : float(voxel.y);
    nextPos.z = ray.dir.z > 0 ? float(voxel.z + 1) : float(voxel.z);

    vec3 tMax = (nextPos - (ray.pos - lightPos)) * ray.invDir;
    vec3 tDelta = abs(ray.invDir);

    vec3 tint = vec3(0);
    float denom = 0;
    uint index = 0u;

    while (any(greaterThanEqual(abs(voxel), ivec3(radius)))) {
        index++;

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

        if (oldVoxel == voxel) {
            return vec3(1);
        }
    }

    uint gridIndex = cellRangeStart + getGridIndex(voxel, radius);

    while (all(lessThan(abs(voxel), ivec3(radius)))) {
        index++;

        ShadowCell cell = shadowGrid[gridIndex];

        if ((cell.data & 1u) == 1u) {
            if (index > 1u) {
                return vec3(0);
            }
        } else {
            uint endIndex = cell.shadowIndex + (cell.data >> 1u);

            for (uint j = cell.shadowIndex; j < endIndex; j++) {
                float dist;
                vec4 outColor;
                ColoredQuad quad = shadows[j];

                if (sampleColoredQuad(true, u_BlockTex, textureSize(u_BlockTex, 0), ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
                    if (outColor.a == 1) {
                        return vec3(0);
                    } else if (outColor.a != 0) {
                        tint += outColor.rgb * outColor.a;
                        denom += outColor.a;
                    }
                }
            }
        }

        ivec3 oldVoxel = voxel;

        if (tMax.x < tMax.y) {
            if (tMax.x < tMax.z) {
                voxel.x += step.x;
                gridIndex += indexStep.x;
                tMax.x += tDelta.x;
            } else {
                voxel.z += step.z;
                gridIndex += indexStep.z;
                tMax.z += tDelta.z;
            }
        } else {
            if (tMax.y < tMax.z) {
                voxel.y += step.y;
                gridIndex += indexStep.y;
                tMax.y += tDelta.y;
            } else {
                voxel.z += step.z;
                gridIndex += indexStep.z;
                tMax.z += tDelta.z;
            }
        }

        if (oldVoxel == voxel) {
            return vec3(1);
        }
    }

    if (denom > 0) {
        return tint / denom;
    } else {
        return vec3(1);
    }
}

vec3 snapVertexPosition() {
    ivec2 textureSize = textureSize(u_BlockTex, 0);
    vec2 texelPos = v_TexCoord * textureSize;
    vec2 d = (floor(texelPos) + 0.5 - texelPos) / textureSize;

    vec2 stepA = dFdx(v_TexCoord);
    vec2 stepB = dFdy(v_TexCoord);
    float det = stepA.x * stepB.y - stepA.y * stepB.x;
    float a = (d.x * stepB.y - d.y * stepB.x) / det;
    float b = (-d.x * stepA.y + d.y * stepA.x) / det;

    return v_Pos + dFdx(v_Pos) * a + dFdy(v_Pos) * b;
}

void main() {
    vec3 texturePos = snapVertexPosition();

    vec4 color = u_UseRGSS ? sampleRGSS(u_BlockTex, v_TexCoord, u_TexelSize) : sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);
    color *= v_Color; // Apply per-vertex color modulator

    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    vec3 lightColor = vec3(0);
    uint sectionRange = lights.sectionRanges[v_SectionPos];
    uint sectionStart = sectionRange >> 16u;
    uint sectionEnd = sectionRange & 0xFFFFu;

    for (uint i = sectionStart; i < sectionEnd; i++) {
        Light light = lights.array[i];

        if (all(lessThan(abs(light.pos - v_Pos), vec3(light.radius)))) {
            lightColor += samplePointLight(light.pos, v_Pos, light.radius, unpackUnorm4x8(light.color).xyz) * test(ray(light, texturePos), ivec3(floor(light.pos)), light.shadowRadius, light.cellRangeStart);
        }
    }

    fragColor = vec4(color.rgb * lightColor * (1 - total_fog_value(v_FragDistance.y, v_FragDistance.x, u_EnvironmentFog.x, u_EnvironmentFog.y, u_RenderFog.x, u_RenderFog.y)), 0);
}
