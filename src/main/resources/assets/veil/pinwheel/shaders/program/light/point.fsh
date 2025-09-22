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

uniform bool Raytrace = true;
uniform uint NumQuads;

struct Quad {
    vec4 v1, v2, v3, v4;
};

struct QuadGroup {
    uvec4 min, max;
    Quad[64] quads;
};

layout(std430, binding = 0) buffer Quads {
    QuadGroup quadGroups[];
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
    return t > 0.075 && t < m - 0.075;
}

bool raycastQuad(mediump vec3 origin, mediump vec3 dir, mediump float len, Quad q) {
    return raycastTriangle(origin, dir, q.v1.xyz, q.v2.xyz, q.v3.xyz, len) || raycastTriangle(origin, dir, q.v1.xyz, q.v3.xyz, q.v4.xyz, len);
}

bool raycastAABB(mediump vec3 ro, mediump vec3 rd, mediump vec3 bmin, mediump vec3 bmax, mediump float len) {
    mediump vec3 invDir = 1.0 / rd;

    mediump vec3 t0s = (bmin - ro) * invDir;
    mediump vec3 t1s = (bmax - ro) * invDir;

    mediump vec3 tsmaller = min(t0s, t1s);
    mediump vec3 tbigger = max(t0s, t1s);

    mediump float tmin = max(max(tsmaller.x, tsmaller.y), tsmaller.z);
    mediump float tmax = min(min(tbigger.x, tbigger.y), tbigger.z);

    return tmax >= tmin;
}

bool raycastGroup(mediump vec3 origin, mediump vec3 dir, mediump float len, QuadGroup g) {
    if (raycastAABB(origin, dir, vec3(g.min.xyz), vec3(g.max.xyz), len)) {
        for (uint i = 0; i < g.min.w; i++) {
            if (raycastQuad(origin, dir, len, g.quads[i])) {
                return true;
            }
        }
    }

    return false;
}

bool raycastQuads(vec3 origin, vec3 target) {
    if (!Raytrace) {
        return false;
    }

    mediump vec3 delta = target - origin;
    mediump vec3 dir = normalize(delta);
    mediump float len = length(delta);
    uvec2 range = quadRanges[lightID];

    for (uint i = range.x; i <= range.y; i++) {
        QuadGroup g = quadGroups[i];

        if (raycastGroup(origin, dir, len, g)) {
            return true;
        }
    }

    return false;
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / (ScreenSize / 3);

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    vec3 offset = lightPos - pos;

    vec3 normalVS = texture(VeilDynamicNormalSampler, screenUv).xyz;
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
    float diffuse = clamp(0.0, 1.0, dot(normalVS, lightDirection));

    bool shadow = diffuse > 0 && raycastQuads(lightPos, pos);

    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), shadow ? radius / 2 : radius);

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * lightColor;
    fragColor = vec4(diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);

    if (shadow) {
        fragColor /= 2;
    }
}
