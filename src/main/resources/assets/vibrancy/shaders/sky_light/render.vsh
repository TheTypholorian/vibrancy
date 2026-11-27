#version 330 core

#moj_import <sodium:globals.glsl>
#moj_import <sodium:chunk_vertex.glsl>
#moj_import <vibrancy:shadow_map.glsl>

in vec3 a_Normal;

uniform mat4 u_SkyMat;
uniform vec3 u_WorldOffset;
uniform vec3 u_LightOffset;

out vec3 v_ShadowPosition;
out vec2 v_TexCoord;
out vec4 v_Color;
out vec3 v_Normal;

uvec3 _get_relative_chunk_coord(uint pos) {
    // Packing scheme is defined by LocalSectionIndex
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return _get_relative_chunk_coord(pos) * vec3(16.0);
}

void main() {
    _vert_init();

    vec3 lightOffset = vec3(0); // TODO

    vec3 position = _vert_position + _get_draw_translation(_draw_id);

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position + u_WorldOffset, 1.0);
    v_ShadowPosition = (skyLightFisheye((u_SkyMat * vec4(position + u_LightOffset + a_Normal * 1e-3, 1.0)).xyz) + 1) * 0.5;
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_Color = _vert_color;
    v_Normal = a_Normal;
}