#version 330 core

#moj_import <vibrancy:pixel_alignment.glsl>
#moj_import <vibrancy:shadow_map.glsl>

in vec3 v_Position;
in vec2 v_TexCoord;
in vec4 v_Color;
in vec3 v_Normal;

uniform sampler2D u_BlockTex;
uniform sampler2D u_ShadowTex;
uniform uint u_UseTranslucency;
uniform sampler2D u_TranslucentColorTex;
uniform sampler2D u_TranslucentDepthTex;

out vec4 fragColor;

void main() {
    float lightRadius = 16.0; // TODO
    vec3 lightColor = vec3(1, 0.86, 0.6); // TODO

    vec2 atlasSize = textureSize(u_BlockTex, 0);
    vec3 alignedPosition = alignPixel(ivec2(atlasSize), v_TexCoord, v_Position);

    vec4 color = vec4(clamp(1 - length(v_Position) / lightRadius, 0.0, 1.0) * max(0, dot(v_Normal, normalize(-v_Position))) * lightColor, 1) * texture(u_BlockTex, v_TexCoord) * v_Color;

    uint face = directionToFace(alignedPosition);
    vec2 coords = faceAndDirectionToShadowCoords(alignedPosition, face);
    float self = faceAndDirectionToDistance(alignedPosition, face);

    coords = (coords + 1) * 0.5;
    coords.x = (coords.x + float(face)) / 6.0;

    float solidShadow = texture(u_ShadowTex, coords).r * lightRadius;

    if (solidShadow < self) {
        discard;
    }

    if (u_UseTranslucency == 1u) {
        float translucentShadow = texture(u_TranslucentDepthTex, coords).r * lightRadius;

        if (translucentShadow < self) {
            vec4 tint = texture(u_TranslucentColorTex, coords);

            color.rgb = mix(color.rgb * tint.rgb, color.rgb, tint.a);
        }
    }

    fragColor = vec4(color.rgb * color.a, 0);
}