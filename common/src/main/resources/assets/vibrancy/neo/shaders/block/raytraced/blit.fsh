#version 430

#include "vibrancy:include/light_blit"

uniform sampler2D Sampler0;

uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform vec2 ScreenSize;

in vec2 texCoord0;

out vec4 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    float len;
};

Ray check(Quad self, vec2 mappedUV) {
    vec3 pos = interpolateQuadPos(self, mappedUV);

    vec3 delta = LightPos - pos;
    vec3 dir = normalize(delta);
    float len = length(delta);

    return Ray(pos, dir, len);
}

vec4 test(Quad q, Ray check) {
    float dist;

    return sampleQuad(Sampler0, check.pos, check.dir, check.len, 1e-3, q, dist);
}

void main() {
    Quad self;
    vec2 mappedUV;
    vec2 step;

    lightBlitInit(self, mappedUV, step);

    Ray rayA = check(self, mappedUV);
    Ray rayB = check(self, mappedUV + vec2(step.x, 0));
    Ray rayC = check(self, mappedUV + vec2(-step.x, 0));
    Ray rayD = check(self, mappedUV + vec2(0, step.y));
    Ray rayE = check(self, mappedUV + vec2(0, -step.y));

    fragColor = samplePointLight(ScreenSize, LightPos, rayA.pos, LightRadius, LightColor);

    for (uint i = 0u; i < shadowQuads.length(); i++) {
        Quad q = shadowQuads[i];

        vec4 a = test(q, rayA);
        vec4 b = test(q, rayB);
        vec4 c = test(q, rayC);
        vec4 d = test(q, rayD);
        vec4 e = test(q, rayE);

        fragColor *= (a + b + c + d + e) / 5;
    }
}
