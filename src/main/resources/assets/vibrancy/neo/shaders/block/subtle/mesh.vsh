#version 430

struct Light {
    vec3 pos;
    uint shape;
    vec3 color;
    float flicker;
};

layout(std430) buffer LightBuffer {
    Light lights[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;
in uint LightIndex;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
flat out Light light;
out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 fogPosition;
out vec3 vertexNormal;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    light = lights[LightIndex];
    vertexColor = Color;
    fogPosition = pos.xyz;
    vertexPosition = Position;
    vertexNormal = Normal;
}
