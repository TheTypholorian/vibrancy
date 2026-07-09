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
uniform sampler2D u_MaterialTex;
uniform sampler2D u_TransmissionTex;

out vec4 fragColor;

vec3 getWedgeColor(uint wedge) {
    return fract(vec3(
        float(wedge) * 0.754877666,
        float(wedge) * 0.569840296,
        float(wedge) * 0.438289623
    ));
}

uint getWedgeIndex(RaytracedPointLight light, vec3 fragPos) {
    vec3 delta = abs(light.pos - fragPos);

    uint index = 0;

    if (fragPos.x >= light.pos.x) {
        index |= 1u;
    }

    if (fragPos.y >= light.pos.y) {
        index |= 2u;
    }

    if (fragPos.z >= light.pos.z) {
        index |= 4u;
    }

    index *= 6;

    if (delta.x >= delta.y) {
        if (delta.y >= delta.z) {
            index += 0;
        } else if (delta.x >= delta.z) {
            index += 1;
        } else {
            index += 2;
        }
    } else {
        if (delta.y >= delta.z) {
            index += 3;
        } else if (delta.x >= delta.z) {
            index += 4;
        } else {
            index += 5;
        }
    }

    return index;
}

vec3 getRaytracedPointLightColor(RaytracedPointLight light, vec3 fragPos) {
    vec3 delta = light.pos - fragPos;
    float distSq = dot(delta, delta);
    float radiusSq = light.radius * light.radius;

    if (distSq >= radiusSq) {
        return vec3(0);
    }

    float s = sqrt(distSq) / light.radius;
    float a = 1 - s;
    return a * a * a * unpackUnorm4x8(light.color).rgb * light.brightness;
}

vec3 testRaytracedPointLightRay(Ray ray, RaytracedPointLight light, sampler2D transmissionTex, sampler2D materialTex) {
    ivec3 lightVoxel = ivec3(floor(light.pos));

    vec3 tint = vec3(0);
    float denom = 0;
    float multiplier = 1;

    uint wedge = light.wedges[getWedgeIndex(light, ray.pos)];
    uint wedgeStart = wedge >> 18;
    uint wedgeEnd = wedgeStart + (wedge & 16383u);

    for (uint i = wedgeStart; i < wedgeEnd; i++) {
        uint cell = cheese[i];
        bool cellSolid = (cell & 1u) == 1u;

        if (cellSolid) {
            // TODO
            //return vec3(0);
        } else {
            uint cellStart = cell >> 13u;
            uint cellEnd = cellStart + ((cell >> 1u) & 4095u);

            for (uint j = cellStart; j < cellEnd; j++) {
                float denom;
                vec2 uv;
                float dist;
                ColoredQuad quad = shadows[j];

                if (raycastQuad(ray, 1e-3, quad, denom, uv, dist)) {
                    vec2 texUv = interpolateQuadUV(quad, uv);
                    vec4 pixel = sampleNearest(transmissionTex, texUv, u_TexelSize) * interpolateQuadColor(quad, uv);

                    /*
                    if (dda.voxel == ivec3(0)) {
                        vec4 material = sampleNearest(materialTex, texUv, u_TexelSize);
                        float emission = material.g * material.a;

                        if (emission > 0) {
                            multiplier = emission;
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
                    */
                        if (pixel.a == 1) {
                            return vec3(0);
                        } else if (pixel.a != 0) {
                            tint += pixel.rgb * pixel.a;
                            denom += pixel.a;
                        }
                    //}
                }
            }
        }
    }

    if (denom > 0) {
        return tint / denom * multiplier;
    } else {
        return vec3(multiplier);
    }
}

/*
vec3 specularRaytracedPointLight(RaytracedPointLight light, vec3 color, vec3 vertexPos, vec3 cameraPos, vec3 normal, sampler2D materialTex, vec2 texCoord0) {
    vec3 lightDelta = light.pos - vertexPos;
    vec3 cameraDir = normalize(cameraPos - vertexPos);
    vec3 resultDir = 2 * dot(cameraDir, normal) * normal - cameraDir;

    EndlessRay ray = createEndlessRay(vertexPos, resultDir);

    uint cell = shadowGrid[light.cellRangeStart + getShadowGridIndex(light, ivec3(0))];
    uint cellStart = cell >> 13u;
    uint cellEnd = cellStart + ((cell >> 1u) & 4095u);

    float closestHit = -1;
    vec4 lightMaterial = vec4(0);

    for (uint j = cellStart; j < cellEnd; j++) {
        float denom;
        vec2 uv;
        float dist;
        ColoredQuad quad = shadows[j];

        if (raycastQuad(ray, 1e-3, quad, denom, uv, dist) && denom < 0) {
            vec2 texUv = interpolateQuadUV(quad, uv);
            vec4 material = sampleNearest(materialTex, texUv, u_TexelSize);

            if (material.a > 0.5 && (dist < closestHit || closestHit == -1)) {
                closestHit = dist;
                lightMaterial = material;
            }
        }
    }

    if (closestHit == -1) {
        return color;
    }

    vec4 reflectionMaterial = sampleNearest(materialTex, texCoord0, u_TexelSize);
    return color + color * reflectionMaterial.r * reflectionMaterial.a * lightMaterial.g * lightMaterial.a * config.specular.strength;
}
*/

void calculateRaytracedPointLight(RaytracedPointLight light, vec3 fragPos, vec3 shadowPos, vec3 normal, vec2 texCoord0, sampler2D transmissionTex, sampler2D materialTex, inout vec3 totalLightColor) {
    vec3 delta = light.pos - fragPos;

    if (all(lessThan(abs(delta), vec3(light.radius))) && dot(normal, normalize(delta)) > 0) {
        vec3 lightColor = getRaytracedPointLightColor(light, fragPos) * testRaytracedPointLightRay(createRayTo(shadowPos, light.pos), light, transmissionTex, materialTex);
        vec3 specularColor = lightColor;//specularRaytracedPointLight(light, lightColor, shadowPos, CameraBlockPos - CameraOffset, normal, materialTex, texCoord0);

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
        calculateRaytracedPointLight(lights.array[i], v_Pos, shadowPos, v_Normal, v_TexCoord, u_TransmissionTex, u_MaterialTex, totalLightColor);
    }

    fragColor = vec4(color.rgb * color.a * config.visuals.rayBrightness * totalLightColor * getFogScale(), 0);
}
