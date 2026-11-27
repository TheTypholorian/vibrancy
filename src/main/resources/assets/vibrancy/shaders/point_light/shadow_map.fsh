#version 330 core

#moj_import <sodium:chunk_material.glsl>

flat in uint v_Material;
in vec3 v_ShadowCoords;
in vec2 v_TexCoord;
in vec4 v_Color;

uniform sampler2D u_BlockTex;

out vec4 fragColor;

void main() {
    if (abs(v_ShadowCoords.x) > 1.0 || abs(v_ShadowCoords.y) > 1.0) {
        discard;
    }

    fragColor = texture(u_BlockTex, v_TexCoord) * v_Color;

    if (fragColor.a < _material_alpha_cutoff(v_Material)) {
        discard;
    }
}