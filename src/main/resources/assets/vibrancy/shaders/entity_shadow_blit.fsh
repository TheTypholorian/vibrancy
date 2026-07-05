#version 330

uniform sampler2D u_ShadowTex;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    fragColor = texture(u_ShadowTex, texCoord0);
}
