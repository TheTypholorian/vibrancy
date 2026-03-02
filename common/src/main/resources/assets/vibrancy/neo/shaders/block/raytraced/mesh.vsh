#version 430

#include "vibrancy:include/sprite"

layout(std140, binding = 0) buffer SpriteBuffer {
    Sprite sprites[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
out vec2 texCoord1;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    texCoord0 = UV0;
    Sprite sprite = sprites[gl_VertexID / 4];
    int offset = gl_VertexID % 4;

    if (offset == 0) texCoord1 = vec2(sprite.x, sprite.y);
    else if (offset == 1) texCoord1 = vec2(sprite.x + sprite.width, sprite.y);
    else if (offset == 2) texCoord1 = vec2(sprite.x + sprite.width, sprite.y + sprite.height);
    else texCoord1 = vec2(sprite.x, sprite.y + sprite.height);
}
