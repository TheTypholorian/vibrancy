#version 430

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec4 Color;

out vec2 texCoord0;
out vec2 texCoord1;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    texCoord0 = UV0;
    texCoord1 = vec2(UV1);
    vertexColor = Color;
}
