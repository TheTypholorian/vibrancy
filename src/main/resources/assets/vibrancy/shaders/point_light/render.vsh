#version 330 core

#moj_import <sodium:globals.glsl>
#moj_import <sodium:chunk_vertex.glsl>

in vec3 a_Normal;

uniform vec3 u_WorldOffset;
uniform ivec3 u_RelativeOffset;

out vec3 v_Position;
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

    vec3 lightOffset = vec3(0.5); // TODO

    vec3 translation = _get_draw_translation(_draw_id);

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(_vert_position + u_WorldOffset + translation, 1.0);
    v_Position = _vert_position + translation + u_RelativeOffset - lightOffset + a_Normal * 1e-3; // TODO
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_Color = _vert_color;
    v_Normal = a_Normal;
}