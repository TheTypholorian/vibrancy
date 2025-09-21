#version 430 core

#include "veil:common"
#include "veil:deferred_utils"
#include "veil:color_utilities"
#include "veil:light"

flat in int lightID;
in vec3 lightPos;
in vec3 lightColor;
in float radius;

uniform sampler2D VeilDynamicAlbedoSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec2 ScreenSize;

uniform bool Raytrace = true;
uniform uint NumQuads;

struct Quad {
    vec3 v1, v2, v3, v4;
};

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};
layout(std430, binding = 1) buffer QuadRanges {
    uvec2 quadRanges[];
};

bool raycastTriangle(vec3 ro, vec3 rd, vec3 v0, vec3 v1, vec3 v2, float m) {
    vec3 e1 = v1 - v0;
    vec3 e2 = v2 - v0;
    vec3 p  = cross(rd, e2);
    float det = dot(e1, p);
    if (abs(det) < 1e-6) return false;

    float invDet = 1.0 / det;
    vec3 s = ro - v0;
    float u = dot(s, p) * invDet;
    if (u < 0.0 || u > 1.0) return false;

    vec3 q = cross(s, e1);
    float v = dot(rd, q) * invDet;
    if (v < 0.0 || u + v > 1.0) return false;

    float t = dot(e2, q) * invDet;
    return t > 0.0 && t < m;
}

bool raycastQuad(vec3 origin, vec3 dir, float len, Quad q) {
    if (raycastTriangle(origin, dir, q.v1, q.v2, q.v3, len)) {
        return true;
    }

    if (raycastTriangle(origin, dir, q.v1, q.v3, q.v4, len)) {
        return true;
    }

    return false;
}

bool raycastQuads(vec3 origin, vec3 target) {
    if (!Raytrace) {
        return false;
    }

    vec3 delta = target - origin;
    vec3 dir = normalize(delta);
    float len = length(delta);
    uvec2 range = quadRanges[lightID];

    for (uint i = range.x; i <= range.y; i++) {
        Quad q = quads[i];

        if (raycastQuad(origin, dir, len, q)) {
            return true;
        }
    }

    return false;
}

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, screenUv);
    if(albedoColor.a == 0) {
        discard;
    }

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    if (raycastQuads(pos, lightPos)) {
        discard;
    }

    // lighting calculation
    vec3 offset = lightPos - pos;

    vec3 normalVS = texture(VeilDynamicNormalSampler, screenUv).xyz;
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
    float diffuse = clamp(0.0, 1.0, dot(normalVS, lightDirection));
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), radius);

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * lightColor;
    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
}
