#version 430

//#include "big_shot_lib:fog"

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
//uniform sampler2D Sampler1;
uniform ivec2 Sampler0Size;
uniform float LightBrightness;

uniform vec3 CameraPos;

in vec2 texCoord0;
//in vec2 texCoord1;
in vec4 vertexColor;
//in vec3 vertexPosition;
in float vertexDistance;

out vec3 fragColor;

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * Sampler0Size), 0);

    if (block.a == 0) {
        discard;
    }

    fragColor = block.rgb * block.a * vertexColor.rgb * clamp(vertexColor.a, 0, 1) * LightBrightness * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
