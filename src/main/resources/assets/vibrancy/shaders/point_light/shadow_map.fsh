#version 330 core

in vec2 v_TexCoord;
in vec3 v_ShadowCoords;

uniform sampler2D u_BlockTex;

out vec4 fragColor;

void main() {
    if (abs(v_ShadowCoords.x) > 1.0 || abs(v_ShadowCoords.y) > 1.0) {
        discard;
    }

    fragColor = texture(u_BlockTex, v_TexCoord);

    if (fragColor.a < 0.1) {
        discard;
    }
}