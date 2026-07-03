#version 430 core

#include "vibrancy:vertex"
#include "vibrancy:raytraced_point"

out vec3 v_Pos;
out vec3 v_Normal;
out flat uint v_SectionPos;
out vec4 v_Color;
out vec2 v_TexCoord;

void main() {
    _vert_init();

    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
    vec3 position = _vert_position + translation;

    _fog_init(position);

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    v_Pos = _vert_position + lights.worldOffset + _get_draw_translation(_draw_id) + a_VibrancyNormal * 1e-3;
    v_SectionPos = _draw_id;

    v_Normal = a_VibrancyNormal;
    v_Color = _vert_color;
    v_TexCoord = _tex_coord_init();
}
