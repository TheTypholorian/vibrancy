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

bool rayAabb(vec3 origin, vec3 dir, vec3 min, vec3 max) {
    if (all(greaterThanEqual(origin, min)) &&
        all(lessThanEqual(origin, max))) {
        return true;
    }

    vec3 t1 = (min - origin) / dir;
    vec3 t2 = (max - origin) / dir;

    vec3 tmin = min(t1, t2);
    vec3 tmax = max(t1, t2);

    float entry = max(max(tmin.x, tmin.y), tmin.z);
    float exit = min(min(tmax.x, tmax.y), tmax.z);

    return exit >= entry && exit >= 0.0;
}

void main() {
    fragColor = vec4(1);

    vec3 Pos = getWorldPos(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), ScreenSize).xyz;

    if (!rayAabb(Pos, LightDirection, BoxMin, BoxMax)) {
        discard;
    }
}
