#version 430

//#include "big_shot_lib:fog"

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) {
        return length(pos);
    } else {
        float distXZ = length(pos.xz);
        float distY = abs(pos.y);
        return max(distXZ, distY);
    }
}

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform ivec2 Sampler0Size;
uniform float LightBrightness;

uniform vec3 CameraPos;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;

out vec3 fragColor;

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * Sampler0Size), 0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    fragColor = block.rgb * block.a * texelFetch(Sampler1, ivec2(texCoord1), 0).rgb * LightBrightness * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
