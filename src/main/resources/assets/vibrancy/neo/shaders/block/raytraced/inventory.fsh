#version 430

//#include "big_shot_lib:fog"
#include "vibrancy:fragment"
#include "vibrancy:rays"

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform ivec2 Sampler1Size;

uniform vec3 LightColor;
uniform ivec2 LightCoords;
uniform vec3 LightPos;
uniform float LightRadius;
uniform float LightBrightness;

uniform ivec2 ScreenSize;

in vec2 texCoord0;
//in vec4 vertexColor;
in vec3 vertexPosition;
//in vec3 vertexNormal;

out vec4 fragColor;

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
    ivec2 coords = ivec2(gl_FragCoord.xy);
    coords.y = ScreenSize.y - coords.y;

    ivec2 relative = coords - LightCoords;
    float len = clamp(1 - length(relative) / LightRadius, 0, 1);

    vec4 block = texture(Sampler0, texCoord0);
    fragColor = vec4(block.rgb * block.a * LightColor * len * len * LightBrightness, 1);

    Ray ray = ray(vertexPosition);

    vec3 tint = vec3(0);
    float denom = 0;

    for (uint i = 0u; i < shadowQuads.length(); i++) {
        float dist;
        vec4 outColor;
        Quad quad = shadowQuads[i];

        if (sampleQuad(true, Sampler1, Sampler1Size, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
            if (outColor.a == 1) {
                fragColor = vec4(0);
                break;
            } else if (outColor.a != 0) {
                tint += outColor.rgb * outColor.a;
                denom += outColor.a;
            }
        }
    }

    if (denom > 0) {
        fragColor.rgb *= tint / denom;
    }
}
