#version 330 core

#moj_import <vibrancy:shadow_map.glsl>
#moj_import <sodium:chunk_material.glsl>

flat in uint v_Material;
in vec3 v_RelativePos;
in vec2 v_TexCoord;
in vec4 v_Color;

uniform uint u_Face;
uniform sampler2D u_BlockTex;

out vec4 fragColor;

void main() {
    if (u_Face != directionToFace(v_RelativePos)) {
        discard;
    }

    fragColor = texture(u_BlockTex, v_TexCoord) * v_Color;

    if (fragColor.a < _material_alpha_cutoff(v_Material)) {
        discard;
    }
}