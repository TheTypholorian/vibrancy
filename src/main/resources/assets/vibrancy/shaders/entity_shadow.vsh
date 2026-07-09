#version 330

#include "minecraft:dynamictransforms"
#include "minecraft:projection"

in vec3 Position;
in vec2 UV0;
in ivec2 UV2;

out vec3 vertexPosition;
out vec2 texCoord0;
out float vertexLight;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexPosition = Position;
    texCoord0 = UV0;
    vertexLight = float(max(UV2.x, UV2.y)) / 240.0;
}
