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

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform int FogShape;

in vec3 Position;
in vec2 UV0;
in uint LightIndex;
in ivec2 UV1;
in vec4 Color;

out vec2 texCoord0;
out vec2 texCoord1;
flat out uint lightIndex;
out vec4 vertexColor;
out vec3 vertexPosition;
out float vertexDistance;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    texCoord1 = vec2(UV1);
    lightIndex = LightIndex;
    vertexColor = Color;
    vertexDistance = fog_distance(pos.xyz, FogShape);
    vertexPosition = Position;
}
