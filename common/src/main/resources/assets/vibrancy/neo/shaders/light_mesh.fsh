#version 150

#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 CameraPos;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;

out vec4 fragColor;

void main() {
    fragColor = bigShotFog(texture(Sampler0, texCoord0) * texelFetch(Sampler1, ivec2(texCoord1), 0) * vertexColor, vertexPosition - CameraPos);
}
