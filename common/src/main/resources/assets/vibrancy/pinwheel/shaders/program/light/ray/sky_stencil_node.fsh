#version 430

#include vibrancy:mask_utils
#include vibrancy:quads
#include veil:deferred_utils

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec3 LightDirection;
uniform vec2 ScreenSize;

uniform vec3 BoxMin;
uniform vec3 BoxMax;

out vec4 fragColor;

bool rayAabb(vec3 origin, vec3 invDir, vec3 _min, vec3 _max) {
    if (all(greaterThanEqual(origin, _min)) &&
        all(lessThanEqual(origin, _max))) {
        return true;
    }

    float tmin, tmax, tymin, tymax, tzmin, tzmax;

    tmin = ((invDir.x < 0 ? _max : _min).x - origin.x) * invDir.x;
    tmax = ((invDir.x < 0 ? _min : _max).x - origin.x) * invDir.x;
    tymin = ((invDir.y < 0 ? _max : _min).y - origin.y) * invDir.y;
    tymax = ((invDir.y < 0 ? _min : _max).y - origin.y) * invDir.y;

    if ((tmin > tymax) || (tymin > tmax)) return false;

    if (tymin > tmin) tmin = tymin;
    if (tymax < tmax) tmax = tymax;

    tzmin = ((invDir.z < 0 ? _max : _min).z - origin.z) * invDir.z;
    tzmax = ((invDir.z < 0 ? _min : _max).z - origin.z) * invDir.z;

    if ((tmin > tzmax) || (tzmin > tmax)) return false;

    if (tzmin > tmin) tmin = tzmin;
    if (tzmax < tmax) tmax = tzmax;

    return true;
}

void main() {
    fragColor = vec4(1, 0, 0, 1);

    vec3 Pos = getWorldPos(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), ScreenSize).xyz;

    if (!rayAabb(Pos, 1 / LightDirection, BoxMin, BoxMax)) {
        discard;
    }
}
