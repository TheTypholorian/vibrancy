#version 430 core

#include "veil:common"
#include "veil:deferred_utils"
#include "veil:color_utilities"
#include "veil:light"

flat in int lightID;
in vec3 lightPos;
in vec3 lightColor;
in float radius;

uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPos;
uniform vec2 ScreenSize;
uniform int LightScale = 1;

uniform bool Raytrace = true;

struct Quad {
    vec4 v1, v2, v3, v4;
};

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};
layout(std430, binding = 1) buffer QuadRanges {
    uvec2 quadRanges[];
};

out vec4 fragColor;

float lengthSquared(vec3 vec) {
    return vec.x * vec.x + vec.y * vec.y + vec.z * vec.z;
}

bool raycastTriangle(mediump vec3 ro, mediump vec3 rd, mediump vec3 v0, mediump vec3 v1, mediump vec3 v2, mediump float m) {
    mediump vec3 v1v0 = v1 - v0;
    mediump vec3 v2v0 = v2 - v0;
    mediump vec3 rov0 = ro - v0;
    mediump vec3 n = cross(v1v0, v2v0);
    mediump vec3 q = cross(rov0, rd);
    mediump float d = 1.0 / dot(rd, n);
    mediump float u = d * dot(-q, v2v0);

    if (u < 0) {
        return false;
    }

    mediump float v = d * dot(q, v1v0);

    if (v < 0 || u + v > 1) {
        return false;
    }

    mediump float t = d * dot(-n, rov0);

    return t > 0 && t < m;
}

bool raycastQuad(mediump vec3 origin, mediump vec3 dir, mediump float len, Quad q) {
    return raycastTriangle(origin, dir, q.v1.xyz, q.v2.xyz, q.v3.xyz, len) || raycastTriangle(origin, dir, q.v1.xyz, q.v3.xyz, q.v4.xyz, len);
}

bool raycastQuads(vec3 origin, vec3 target, float margin) {
    if (!Raytrace) {
        return false;
    }

    mediump vec3 delta = target - origin;
    mediump vec3 dir = normalize(delta);
    mediump float len = length(delta);
    uvec2 range = quadRanges[lightID];

    for (uint i = range.x; i <= range.y; i++) {
        Quad q = quads[i];

        if (raycastQuad(origin, dir, len - margin, q)) {
            return true;
        }
    }

    return false;
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / (ScreenSize / LightScale);

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    vec3 offset = lightPos - pos;

    vec3 normalVS = texture(VeilDynamicNormalSampler, screenUv).xyz;
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
    float normalDiff = dot(normalVS, lightDirection);

    if (normalDiff < 0 || raycastQuads(lightPos, pos, 1e-1)) {
        discard;
    }

    float diffuse = clamp(0.0, 1.0, normalDiff);

    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), radius);

    float reflectivity = 0.05;
    vec4 diffuseColor = diffuse * vec4(lightColor, 1);
    fragColor = diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity;

    gl_FragDepth = depth;
}
