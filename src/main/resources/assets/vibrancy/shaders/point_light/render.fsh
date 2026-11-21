#version 330 core

#moj_import <vibrancy:pixel_alignment.glsl>
#moj_import <vibrancy:shadow_map.glsl>

in vec3 v_Position;
in vec2 v_TexCoord;
in vec4 v_Color;

uniform sampler2D u_BlockTex;
uniform sampler2D u_ShadowTex;

out vec4 fragColor;

void main() {
    float lightRadius = 6.0; // TODO
    vec2 atlasSize = textureSize(u_BlockTex, 0);
    vec3 alignedPosition = alignPixel(ivec2(atlasSize), v_TexCoord, v_Position);

    fragColor = vec4(clamp(1 - length(v_Position) / lightRadius, 0.0, 1.0) * vec3(1, 0.5, 0.25), 1) * texture(u_BlockTex, v_TexCoord) * v_Color;

    if (fragColor.a < 0.1) {
        discard;
    }

    uint face;
    vec2 coords = (directionToShadowCoords(alignedPosition, face) + 1) * 0.5;

    //vec2 shadowSize = textureSize(u_ShadowTex, 0);
    //coords *= (shadowSize.y - 1) / shadowSize.y;

    coords.x = (coords.x + float(face)) / 6.0;

    float shadow = texture(u_ShadowTex, coords).r;
    float self = faceAndDirectionToDistance(alignedPosition, face) / lightRadius;

    if (shadow < self) {
        discard;
    }
}