#version 430

uniform mat4 ProjMat;
uniform mat4 ViewMat;

in vec3 Position;

out vec3 pos;

void main() {
    vec4 Pos = vec4(Position, 1);
    gl_Position = ProjMat * Pos;
    pos = Pos.xyz;
}
