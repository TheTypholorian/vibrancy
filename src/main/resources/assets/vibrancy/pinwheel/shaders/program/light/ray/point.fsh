#version 430

#include "veil:common"
#include "veil:deferred_utils"
#include "veil:color_utilities"
#include "veil:light"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskSampler;
uniform sampler2D ShadowMaskDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D VeilDynamicAlbedoSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform bool AnyShadows;

out vec4 fragColor;

void applyShadowColor(vec4 color) {
    if (color.a == 1) {
        discard;
    } else {
        // wip code for blocks tinting light
        //fragColor = vec4(fragColor.rgb * mix(color.rgb, vec3(0), color.a), fragColor.a);
    }
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, screenUv);

    if (albedoColor.a == 0) {
        discard;
    }

    float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    vec3 offset = LightPos - pos;

    vec3 normalVS = texelFetch(VeilDynamicNormalSampler, ivec2(gl_FragCoord.xy), 0).xyz;
    vec3 lightDirection = normalize((VeilCamera.ViewMat * vec4(offset, 0.0)).xyz);
    float normalD = dot(normalVS, lightDirection);
    float diffuse = clamp(0.0, 1.0, normalD);
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), LightRadius);

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * LightColor;
    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);

    if (AnyShadows) {
        // raytracing
        float maskDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskDepthSampler, ivec2(gl_FragCoord.xy), 0).r);
        float worldDepth = depthSampleToWorldDepth(depth);

        if (worldDepth > maskDepth - 1e-3) {
            applyShadowColor(texelFetch(ShadowMaskSampler, ivec2(gl_FragCoord.xy), 0));
        }
    }
}
