#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int ShadowScale;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
out vec2 texCoord1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    texCoord0 = UV0;
    float minX = floor(gl_VertexID / 4.0) * ShadowScale;
    float maxX = minX + ShadowScale;
    int quadOffset = gl_VertexID % 4;

    if (quadOffset == 0) texCoord1 = vec2(minX, 0);
    if (quadOffset == 1) texCoord1 = vec2(maxX, 0);
    if (quadOffset == 2) texCoord1 = vec2(maxX, ShadowScale);
    if (quadOffset == 3) texCoord1 = vec2(minX, ShadowScale);
}
