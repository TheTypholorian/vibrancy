#version 330 core

#moj_import <vibrancy:shadow_map.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 v_TexCoord;
out vec3 v_ShadowCoords;

void main() {
    float lightRadius = 16.0; // TODO
    vec3 lightOffset = vec3(0.5, 0.5, 0.5); // TODO

    uint face = uint(gl_InstanceID);
    vec3 dir = (Position);
    float z = faceAndDirectionToDistance(dir, face) / lightRadius;
    vec2 coords = faceAndDirectionToShadowCoords(dir, face);

    v_ShadowCoords = vec3(coords, z);

    gl_Position = vec4((coords.x + 1.0 + 2.0 * float(face)) / 6.0 - 1.0, coords.y, z * 2 - 1, 1);
    v_TexCoord = UV0;
}