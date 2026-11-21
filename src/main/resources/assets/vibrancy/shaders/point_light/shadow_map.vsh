#version 330 core

#moj_import <vibrancy:shadow_map.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 v_TexCoord;
out vec3 v_ShadowCoords;

void main() {
    uint face = uint(gl_InstanceID);
    vec3 dir = (Position - 8.0) / 8; // [-1, 1]
    float z = faceAndDirectionToDistance(dir, face);
    vec2 coords = faceAndDirectionToShadowCoords(dir, face);

    v_ShadowCoords = vec3(coords, z);

    gl_Position = vec4((coords.x + 1.0 + 2.0 * float(face)) / 6.0 - 1.0, coords.y, z, 1);
    v_TexCoord = UV0;
}