#version 430 core

#include "minecraft:globals"
#include "vibrancy:fragment"
#include "vibrancy:pixel_alignment"
#include "vibrancy:raytraced_point"

in vec3 v_Pos;
in vec3 v_Normal;
in flat uint v_SectionPos;
in vec4 v_Color;
in vec2 v_TexCoord;

uniform sampler2D u_BlockTex;
uniform sampler2D u_ReflectionTex;
uniform sampler2D u_TransmissionTex;

out vec4 fragColor;

vec3 getRaytracedPointLightColor(RaytracedPointLight light, vec3 fragPos) {
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

vec3 testRaytracedPointLightRay(Ray ray, RaytracedPointLight light, sampler2D transmissionTex, sampler2D reflectionTex) {
    ivec3 lightVoxel = ivec3(floor(light.pos)) >> SHADOW_GRID_SHIFT;
    DDAState dda = createDDA(ray, worldPosToShadowGridRelative(light, ray.pos), clamp(worldPosToShadowGridRelative(light, ivec3(floor(ray.pos))), ivec3(-light.shadowRadius), ivec3(light.shadowRadius)));
    ivec3 indexStep = getShadowGridIncrement(light, dda);

    vec3 tint = vec3(0);
    float denom = 0;
    float multiplier = 1;

    uint gridIndex = light.cellRangeStart + getShadowGridIndex(light, dda.voxel);

    while (all(lessThanEqual(abs(dda.voxel), ivec3(light.shadowRadius))) && dda.voxel != ivec3(0)) { // TODO make testing center voxel configurable
        ShadowGridCell cell = shadowGrid[gridIndex];

        for (uint voxelIndex = 0u; voxelIndex < SHADOW_GRID_AREA; voxelIndex++) {
            uint voxel = cell.voxels[voxelIndex];
            bool voxelSolid = (voxel & 1u) == 1u;

            if (!config.visuals.alignPixels || dda.tExit - dda.tEnter > 1e-3) {
                if (voxelSolid) {
                    // TODO
                    //return vec3(0);
                } else {
                    uint voxelStart = voxel >> 13u;
                    uint voxelEnd = voxelStart + ((voxel >> 1u) & 4095u);

                    for (uint j = voxelStart; j < voxelEnd; j++) {
                        float denom;
                        vec2 uv;
                        float dist;
                        ColoredQuad quad = shadows[j];

                        if (raycastQuad(ray, 1e-3, quad, denom, uv, dist)) {
                            vec2 texUv = interpolateQuadUV(quad, uv);
                            vec4 pixel = sampleNearest(transmissionTex, texUv, u_TexelSize) * interpolateQuadColor(quad, uv);

                            if (dda.voxel == ivec3(0)) {
                                float emission = sampleNearest(reflectionTex, texUv, u_TexelSize).g;

                                if (emission > 0) {
                                    multiplier = emission;
                                    // TODO remove breaking here
                                    break; // TODO sort properly
                                } else {
                                    if (pixel.a == 1) {
                                        return vec3(0);
                                    } else if (pixel.a != 0) {
                                        tint += pixel.rgb * pixel.a;
                                        denom += pixel.a;
                                    }
                                }
                            } else {
                                if (pixel.a == 1) {
                                    return vec3(0);
                                } else if (pixel.a != 0) {
                                    tint += pixel.rgb * pixel.a;
                                    denom += pixel.a;
                                }
                            }
                        }
                    }
                }
            }
        }

        stepDDA(dda, gridIndex, indexStep);
    }

    if (denom > 0) {
        return tint / denom * multiplier;
    } else {
        return vec3(multiplier);
    }
}

vec3 specularRaytracedPointLight(RaytracedPointLight light, vec3 color, vec3 vertexPos, vec3 cameraPos, vec3 normal, sampler2D reflectionTex, vec2 texCoord0) {
    vec3 lightDelta = light.pos - vertexPos;
    vec3 cameraDir = normalize(cameraPos - vertexPos);
    vec3 resultDir = 2 * dot(cameraDir, normal) * normal - cameraDir;

    EndlessRay ray = createEndlessRay(vertexPos, resultDir);

    float multiplier = 0;

    uint cell = shadowGrid[light.cellRangeStart + getShadowGridIndex(light, ivec3(0))].voxels[0]; // TODO
    uint cellStart = cell >> 13u;
    uint cellEnd = cellStart + ((cell >> 1u) & 4095u);

    for (uint j = cellStart; j < cellEnd; j++) {
        float denom;
        vec2 uv;
        float dist;
        ColoredQuad quad = shadows[j];

        if (raycastQuad(ray, 1e-3, quad, denom, uv, dist) && denom < 0) {
            vec2 texUv = interpolateQuadUV(quad, uv);
            float emission = sampleNearest(reflectionTex, texUv, u_TexelSize).g;

            if (emission > 0) {
                multiplier = emission;
                break; // TODO sort properly
            }
        }
    }

    return color + color * texture(reflectionTex, texCoord0).r * multiplier * config.specular.strength;
}

void calculateRaytracedPointLight(RaytracedPointLight light, vec3 fragPos, vec3 shadowPos, vec3 normal, vec2 texCoord0, sampler2D transmissionTex, sampler2D reflectionTex, inout vec3 totalLightColor) {
    vec3 delta = light.pos - fragPos;

    if (all(lessThan(abs(delta), vec3(light.radius))) && dot(normal, normalize(delta)) > 0) {
        vec3 lightColor = getRaytracedPointLightColor(light, fragPos) * testRaytracedPointLightRay(createRayTo(shadowPos, light.pos), light, transmissionTex, reflectionTex);
        vec3 specularColor = specularRaytracedPointLight(light, lightColor, shadowPos, CameraBlockPos - CameraOffset, normal, reflectionTex, texCoord0);

        if (config.visuals.limitBrightness) {
            totalLightColor = max(specularColor, totalLightColor);
        } else {
            totalLightColor += specularColor;
        }
    }
}

void main() {
    vec3 shadowPos = getShadowPosition(textureSize(u_TransmissionTex, 0), v_TexCoord, v_Pos);
    vec4 color = sampleTexture(u_BlockTex, v_TexCoord, u_TexelSize) * v_Color;

    vec3 totalLightColor = vec3(0);
    uint sectionRange = lights.sectionRanges[v_SectionPos];

    for (uint i = sectionRange >> 16u; i < (sectionRange & 0xFFFFu); i++) {
        calculateRaytracedPointLight(lights.array[i], v_Pos, shadowPos, v_Normal, v_TexCoord, u_TransmissionTex, u_ReflectionTex, totalLightColor);
    }

    fragColor = vec4(color.rgb * color.a * config.visuals.rayBrightness * totalLightColor * getFogScale(), 0);
}
