#version 440

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 TexCoord;

out vec2 tc;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    tc = TexCoord;
}
