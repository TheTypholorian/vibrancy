#version 430

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 TexCoord;
in vec3 Normal;

out vec2 tc;
out vec3 n;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    tc = TexCoord;
    n = Normal;
}
