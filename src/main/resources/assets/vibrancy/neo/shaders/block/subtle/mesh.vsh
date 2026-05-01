#version 430

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) {
        return length(pos);
    } else {
        float distXZ = length(pos.xz);
        float distY = abs(pos.y);
        return max(distXZ, distY);
    }
}

struct Light {
    vec3 pos;
    uint shape;
    vec3 color;
};

layout(std430, binding = 0) buffer LightBuffer {
    Light lights[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform int FogShape;

in vec3 Position;
in vec2 UV0;
in uint LightIndex;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
flat out Light light;
out vec4 vertexColor;
out vec3 vertexPosition;
out float vertexDistance;
out vec3 vertexNormal;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    light = lights[LightIndex];
    vertexColor = Color;
    vertexDistance = fog_distance(pos.xyz, FogShape);
    vertexPosition = Position;
    vertexNormal = Normal;
}
