#version 430

#include "vibrancy:common"
#include "vibrancy:fragment"
#include "veil:common"
#include "veil:space_helper"
#include "veil:light"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskSampler;
uniform sampler2D VeilDynamicNormalSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void main() {
    vec3 pos = getWorldPos(DiffuseDepthSampler, ScreenSize);

        /*
    vec4 color = texelFetch(ShadowMaskSampler, ivec2(gl_FragCoord.xy), 0);

    if (color.r == 1) {
        discard;
    }
*/

    fragColor = sampleLight(VeilDynamicNormalSampler, LightPos, pos, LightRadius, LightColor);
}
