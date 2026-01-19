#version 430

#include "vibrancy:include/fragment"

uniform sampler2D DiffuseDepthSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = getWorldPos(DiffuseDepthSampler, ScreenSize, IProjMat, IModelMat, CameraPos, gl_FragCoord.xy / ScreenSize);
}
