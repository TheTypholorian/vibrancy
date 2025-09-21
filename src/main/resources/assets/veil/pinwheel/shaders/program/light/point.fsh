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

out vec4 fragColor;

bool raycastTriangle(vec3 ro, vec3 rd, vec3 v0, vec3 v1, vec3 v2, float m) {
    vec3 v1v0 = v1 - v0;
    vec3 v2v0 = v2 - v0;
    vec3 rov0 = ro - v0;
    vec3  n = cross( v1v0, v2v0 );
    vec3  q = cross( rov0, rd );
    float d = 1.0/dot( rd, n );
    float u = d*dot( -q, v2v0 );
    float v = d*dot(  q, v1v0 );
    float t = d*dot( -n, rov0 );
    if( u<0.0 || v<0.0 || (u+v)>1.0 ) t = -1.0;
    return t > 1e-3 && t < m;
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

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, screenUv);
    if(albedoColor.a == 0) {
        discard;
    }

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

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

    if (raycastQuads(pos, lightPos)) {
        discard;
    }
}
