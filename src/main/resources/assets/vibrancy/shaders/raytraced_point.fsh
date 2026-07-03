#version 430 core

#include "vibrancy:fragment"
#include "vibrancy:raytraced_point"
#include "vibrancy:pixel_alignment"

in vec3 v_Pos;
in vec3 v_Normal;
in flat uint v_SectionPos;
in vec4 v_Color;
in vec2 v_TexCoord;

uniform sampler2D u_BlockTex;
//uniform sampler2D u_ReflectionTex;
uniform sampler2D u_TransmissionTex;

out vec4 fragColor;

uint getGridIndex(ivec3 voxel, uint radius, uint size) {
    ivec3 voxel1 = voxel + ivec3(radius);
    return uint((voxel1.x * size + voxel1.y) * size + voxel1.z);
}

vec3 testRay(Ray ray, ivec3 lightPos, uint radius, uint cellRangeStart) {
    DDAState dda = createDDA(ray, ray.pos - lightPos, clamp(ivec3(floor(ray.pos) - lightPos), ivec3(-radius), ivec3(radius)));
    uint gridSize = radius * 2 + 1;
    ivec3 indexStep = dda.step * ivec3(gridSize * gridSize, gridSize, 1);

    vec3 tint = vec3(0);
    float denom = 0;

    uint gridIndex = cellRangeStart + getGridIndex(dda.voxel, radius, gridSize);

    while (all(lessThanEqual(abs(dda.voxel), ivec3(radius)))) {
        uint cell = shadowGrid[gridIndex];
        bool cellSolid = (cell & 1u) == 1u;

        if (cellSolid) {
            if (!config.visuals.alignPixels || dda.tExit - dda.tEnter > 1e-3) {
                return vec3(0);
            }
        } else {
            uint cellStart = cell >> 13u;
            uint cellEnd = cellStart + ((cell >> 1u) & 4095u);

            for (uint j = cellStart; j < cellEnd; j++) {
                float denom;
                vec2 uv;
                float dist;
                ColoredQuad quad = shadows[j];

                if (raycastQuad(ray, 1e-3, quad, denom, uv, dist)) {
                    vec4 pixel = sampleNearest(u_TransmissionTex, interpolateQuadUV(quad, uv), u_TexelSize) * interpolateQuadColor(quad, uv);

                    if (pixel.a == 1) {
                        return vec3(0);
                    } else if (pixel.a != 0) {
                        tint += pixel.rgb * pixel.a;
                        denom += pixel.a;
                    }
                }
            }
        }

        if (dda.voxel == ivec3(0)) {
            break;
        }

        ivec3 oldVoxel = dda.voxel;

        stepDDA(dda, gridIndex, indexStep);

        if (oldVoxel == dda.voxel) {
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
    vec3 shadowPos = getShadowPosition(textureSize(u_TransmissionTex, 0), v_TexCoord, v_Pos);
    vec4 color = sampleTexture(u_BlockTex, v_TexCoord, u_TexelSize) * v_Color;

    vec3 totalLightColor = vec3(0);
    uint sectionRange = lights.sectionRanges[v_SectionPos];

    for (uint i = sectionRange >> 16u; i < (sectionRange & 0xFFFFu); i++) {
        RaytracedPointLight light = lights.array[i];
        vec3 delta = light.pos - v_Pos;

        if (all(lessThan(abs(delta), vec3(light.radius))) && dot(v_Normal, normalize(delta)) > 0) {
            totalLightColor += sampleRaytracedPointLight(light, v_Pos) * testRay(createRayTo(shadowPos, light.pos), ivec3(floor(light.pos)), light.shadowRadius, light.cellRangeStart);
        }
    }

    fragColor = vec4(color.rgb * color.a * config.visuals.rayBrightness * totalLightColor * getFogScale(), 0);
}
