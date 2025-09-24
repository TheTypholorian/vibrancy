#version 430

#include "veil:deferred_utils"

struct Quad {
    vec4 v1, v2, v3, v4;
};

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

bool raycastTriangle(vec3 ro, vec3 rd, vec3 v0, vec3 v1, vec3 v2, float m) {
    vec3 v1v0 = v1 - v0;
    vec3 v2v0 = v2 - v0;
    vec3 rov0 = ro - v0;
    vec3 n = cross(v1v0, v2v0);
    vec3 q = cross(rov0, rd);
    float d = 1.0 / dot(rd, n);
    float u = d * dot(-q, v2v0);

    if (u < 0) {
        return false;
    }

    float v = d * dot(q, v1v0);

    if (v < 0 || u + v > 1) {
        return false;
    }

    float t = d * dot(-n, rov0);

    return t > 1e-3 && t < m - 1e-3;
}

bool raycastQuad(vec3 origin, vec3 dir, float len, Quad q) {
    return raycastTriangle(origin, dir, q.v1.xyz, q.v2.xyz, q.v3.xyz, len) || raycastTriangle(origin, dir, q.v1.xyz, q.v3.xyz, q.v4.xyz, len);
}

uniform sampler2D DiffuseDepthSampler;
uniform vec3 LightPos;
uniform vec2 ScreenSize;

in flat uint index;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec3 Pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    vec3 delta = Pos - LightPos;
    float len = length(delta);
    vec3 dir = delta / len;

    if (!raycastQuad(LightPos, dir, len, quads[index])) {
        discard;
    }
}
