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

struct Check {
    vec3 pos;
    vec3 dir;
    float len;
};

Check check(Quad self, vec2 mappedUV) {
    vec3 pos = interpolateQuadPos(self, mappedUV);

    vec3 delta = LightPos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);

    return Check(pos, dir, len);
}

vec4 test(Quad q, Check check) {
    float dist;

    return sampleQuad(Sampler0, check.pos, check.dir, check.len, 1e-3, q, dist);
}

void main() {
    uint index = uint(floor(texCoord0.x * ShadowWidth));
    Quad self = quads[index];
    vec2 mappedUV = vec2((texCoord0.x - float(index) / ShadowWidth) * ShadowWidth, texCoord0.y);

    float step = 1.0 / 32.0;
    Check checkA = check(self, mappedUV);
    Check checkB = check(self, mappedUV + vec2(step, 0));
    Check checkC = check(self, mappedUV + vec2(-step, 0));
    Check checkD = check(self, mappedUV + vec2(0, step));
    Check checkE = check(self, mappedUV + vec2(0, -step));

    fragColor = samplePointLight(ScreenSize, LightPos, checkA.pos, LightRadius, LightColor);

    for (uint i = 0u; i < quads.length(); i++) {
        if (i != index) {
            Quad q = quads[i];

            vec4 a = test(q, checkA);
            vec4 b = test(q, checkB);
            vec4 c = test(q, checkC);
            vec4 d = test(q, checkD);
            vec4 e = test(q, checkE);

            fragColor *= (a + b + c + d + e) / 5;
        }
    }
}
