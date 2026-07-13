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
    ivec3 startVoxel = ivec3(floor(ray.pos)) - lightVoxel;
    ivec3 endVoxel = clamp(
        ivec3(floor(ray.target)) - lightVoxel,
        ivec3(-light.shadowRadius),
        ivec3(light.shadowRadius)
    );
    DDAState dda = createDDA(ray, ray.pos - lightVoxel, clamp(startVoxel, ivec3(-light.shadowRadius), ivec3(light.shadowRadius)));

    if (dda.voxel == startVoxel) {
        stepDDA(dda);
    }

    for (uint steps = 0; steps < 2000u; steps++) {
        //if (dda.tExit - dda.tEnter > 1e-3) {
            uint cell = shadowGrid[light.cellRangeStart + getShadowGridIndex(light, dda.voxel)];

            switch (cell) {
                case 0u:
                    break;
                case 1u:
                    return vec3(0);
                default:
                    uint cellStart = (cell - 2) >> 13u;
                    uint cellEnd = cellStart + ((cell - 2) & 8191u);

                    for (uint j = cellStart; j < cellEnd; j++) {
                        vec2 uv;
                        ColoredQuad quad = shadows[j];

                        if (raycastQuad(ray, 1e-3, quad, uv)) {
                            return vec3(0);
                        }
                    }
            }
        //}

        if (dda.voxel == endVoxel) {
            return vec3(1);
        }

        stepDDA(dda);
    }
}

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

        if (raycastQuad(ray, 1e-3, quad, denom, uv, dist)) {
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

void calculateRaytracedPointLight(RaytracedPointLight light, vec3 fragPos, vec3 shadowPos, vec3 normal, vec2 texCoord0, sampler2D transmissionTex, sampler2D materialTex, inout vec3 totalLightColor) {
    vec3 lightColor = getRaytracedPointLightColor(light, fragPos) * testRaytracedPointLightRay(createRayTo(light.pos, shadowPos), light, transmissionTex, materialTex);
    vec3 specularColor = specularRaytracedPointLight(light, lightColor, shadowPos, CameraBlockPos - CameraOffset, normal, materialTex, texCoord0);

    if (config.visuals.limitBrightness) {
        totalLightColor = max(specularColor, totalLightColor);
    } else {
        totalLightColor += specularColor;
    }
}

void main() {
    vec3 shadowPos = getShadowPosition(textureSize(u_TransmissionTex, 0), v_TexCoord, v_Pos);
    vec4 color = sampleTexture(u_BlockTex, v_TexCoord, u_TexelSize) * v_Color;

    vec3 totalLightColor = vec3(0);
    uint sectionRange = lights.sectionRanges[v_SectionPos];
    uint rangeStart = sectionRange >> 16u;
    uint rangeEnd = rangeStart + (sectionRange & 0xFFFFu);

    //for (uint i = sectionRange >> 16u; i < min((sectionRange & 0xFFFFu), (sectionRange >> 16u) + 1u); i++) {
    for (uint i = rangeStart; i < rangeEnd; i++) {
        RaytracedPointLight light = lights.array[i];
        vec3 delta = light.pos - v_Pos;

        if (dot(delta, delta) < light.radius * light.radius && dot(v_Normal, delta) > 0) {
            calculateRaytracedPointLight(light, v_Pos, shadowPos, v_Normal, v_TexCoord, u_TransmissionTex, u_MaterialTex, totalLightColor);
        }
    }

    fragColor = vec4(color.rgb * color.a * config.visuals.rayBrightness * totalLightColor * getFogScale(), 0);
}
