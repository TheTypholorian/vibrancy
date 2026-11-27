#version 330 core

#moj_import <vibrancy:shadow_map.glsl>
#moj_import <sodium:globals.glsl>
#moj_import <sodium:chunk_vertex.glsl>

in vec3 a_Normal;

uniform ivec3 u_RelativeOffset;
uniform uint u_Face;

flat out uint v_Material;
out vec3 v_RelativePos;
out vec2 v_TexCoord;
out vec4 v_Color;

uvec3 _get_relative_chunk_coord(uint pos) {
    // Packing scheme is defined by LocalSectionIndex
    return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return _get_relative_chunk_coord(pos) * vec3(16.0);
}

void main() {
    _vert_init();

    float lightRadius = 16.0; // TODO
    vec3 lightOffset = vec3(0.5); // TODO

    vec3 dir = _vert_position + _get_draw_translation(_draw_id) + u_RelativeOffset - lightOffset;
    vec2 coords = faceAndDirectionToShadowCoords(dir, u_Face);
    float distance = faceAndDirectionToDistance(dir, u_Face) / lightRadius;

    gl_Position = vec4(faceRelativeToAbsolute(coords, u_Face), distance * 2 - 1, 1);
    v_Material = _material_params;
    v_RelativePos = dir;
    v_TexCoord = (_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord;
    v_Color = _vert_color;
}