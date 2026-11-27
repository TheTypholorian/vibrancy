#version 330 core

#moj_import <vibrancy:pixel_alignment.glsl>
#moj_import <vibrancy:shadow_map.glsl>

in vec3 v_ShadowPosition;
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
    vec3 lightColor = vec3(1, 0.86, 0.6); // TODO
    float zOffset = 1e-3; // TODO

    vec2 atlasSize = textureSize(u_BlockTex, 0);
    vec3 alignedPosition = alignPixel(ivec2(atlasSize), v_TexCoord, v_ShadowPosition);

    vec4 color = vec4(lightColor, 1) * texture(u_BlockTex, v_TexCoord) * v_Color;

    float solidShadow = texture(u_ShadowTex, alignedPosition.xy).r;

    if (solidShadow < alignedPosition.z - zOffset) {
        discard;
    }

    if (u_UseTranslucency == 1u) {
        float translucentShadow = texture(u_TranslucentDepthTex, alignedPosition.xy).r;

        if (translucentShadow < alignedPosition.z - zOffset) {
            vec4 tint = texture(u_TranslucentColorTex, alignedPosition.xy);

            color.rgb = mix(color.rgb * tint.rgb, color.rgb, tint.a);
        }
    }

    fragColor = vec4(color.rgb * color.a, 0);
}