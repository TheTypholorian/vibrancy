#version 430

#include "veil:common"
#include "veil:deferred_utils"
#include "veil:color_utilities"
#include "veil:light"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskBackSampler;
uniform sampler2D ShadowMaskBackDepthSampler;
uniform sampler2D ShadowMaskFrontDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D VeilDynamicAlbedoSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void applyShadowColor(vec4 color) {
    if (color.a == 0) {
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
    float diffuse = clamp(0.0, 1.0, dot(normalVS, lightDirection));
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), LightRadius);

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * LightColor;
    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);

    // raytracing
    float maskBackDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskBackDepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float maskFrontDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskFrontDepthSampler, ivec2(gl_FragCoord.xy), 0).r);

    float worldDepth = depthSampleToWorldDepth(depth);

    if (maskFrontDepth > maskBackDepth) {
        if (worldDepth < maskBackDepth - 1e-3) {
            applyShadowColor(texelFetch(ShadowMaskBackSampler, ivec2(gl_FragCoord.xy), 0));
        }
    } else {
        if (worldDepth > maskFrontDepth + 1e-3 && worldDepth < maskBackDepth - 1e-3) {
            applyShadowColor(texelFetch(ShadowMaskBackSampler, ivec2(gl_FragCoord.xy), 0));
        }
    }
}
