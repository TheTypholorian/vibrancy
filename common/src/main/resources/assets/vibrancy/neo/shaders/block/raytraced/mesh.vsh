#version 430

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Offset;

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
out vec2 texCoord1;
out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 vertexNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + Offset, 1);
    texCoord0 = UV0;
    texCoord1 = vec2(UV1);
    vertexColor = Color;
    vertexPosition = Position;
    vertexNormal = Normal;
}
