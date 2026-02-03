#version 430

uniform mat4 ModelViewMat;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;
out float vertexDistance;

void main() {
    gl_Position = ModelViewMat * vec4((Position + 32) / 32, 1);
    texCoord = UV0;
    vertexDistance = gl_Position.z;
    gl_Position.xy = gl_Position.xy * 2 - 1;
    gl_Position.z = 0;
}
