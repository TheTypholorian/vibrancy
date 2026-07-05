#version 330

#include "minecraft:dynamictransforms"
#include "minecraft:projection"

in vec3 Position;
in vec2 UV0;

out vec3 vertexPosition;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexPosition = Position;
    texCoord0 = UV0;
}
