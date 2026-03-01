#version 430

#include "vibrancy:include/rays"
#include "vibrancy:include/fragment"

layout(std430, binding = 0) buffer QuadBuffer {
    Quad quads[];
};

uniform sampler2D Sampler0;

uniform int ShadowWidth;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform vec2 ScreenSize;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    uint index = uint(floor(texCoord0.x * ShadowWidth));

    vec2 mappedUV = vec2((texCoord0.x - float(index) / ShadowWidth) * ShadowWidth, texCoord0.y);
    vec3 pos = interpolateQuadPos(quads[index], mappedUV);

    vec3 delta = pos - LightPos;
    vec3 dir = normalize(delta);
    float len = length(delta);

    fragColor = samplePointLight(ScreenSize, LightPos, pos, LightRadius, LightColor);

    for (uint i = 0u; i < quads.length(); i++) {
        if (i != index) {
            float dist;

            fragColor *= sampleQuad(Sampler0, LightPos, dir, len, 1e-3, quads[i], dist);
        }
    }

    fragColor.rgb *= fragColor.a;
}
