#version 150

in vec4 vertexColor;

out vec3 fragColor;

void main() {
    fragColor = vec3(1, 0.5, 0.25) * vertexColor.rgb * vertexColor.a;
}
