#version 430

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV;

out vec2 uv;

void main() {
    gl_Position = vec4(Position, 1);
    uv = UV;
}
