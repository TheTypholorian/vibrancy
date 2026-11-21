#version 330 core

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec3 v_Position;
out vec2 v_TexCoord;
out vec4 v_Color;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    v_Position = Position;
    v_TexCoord = UV0;
    v_Color = Color;
}