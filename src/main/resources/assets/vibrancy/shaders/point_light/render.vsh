#version 330 core

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform u_LightInfo {
    ivec3 u_LightPos;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec3 v_Position;
out vec2 v_TexCoord;
out vec4 v_Color;
out vec3 v_Normal;

void main() {
    vec3 lightOffset = vec3(0.5, 0.5, 0.5); // TODO

    gl_Position = ProjMat * ModelViewMat * vec4(Position + u_LightPos /* TODO */, 1.0);
    v_Position = Position + Normal * 1e-3; // TODO
    v_TexCoord = UV0;
    v_Color = Color;
    v_Normal = Normal;
}