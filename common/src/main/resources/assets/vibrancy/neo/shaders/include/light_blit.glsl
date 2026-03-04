#include "vibrancy:include/rays"
#include "vibrancy:include/fragment"
#include "vibrancy:include/sprite"

layout(std430, binding = 0) buffer LightQuadBuffer {
    Quad lightQuads[];
};
layout(std430, binding = 1) buffer SpriteBuffer {
    Sprite sprites[];
};

bool findSprite(ivec2 spriteCoords, out uint index, out Sprite sprite) {
    for (uint i = 0u; i < sprites.length(); i++) {
        Sprite check = sprites[i];

        if (spriteContains(check, spriteCoords)) {
            index = i;
            sprite = check;
            return true;
        }
    }

    return false;
}

void lightBlitInit(out Quad self, out vec2 mappedUV, out vec2 antiAliasStep) {
    ivec2 spriteCoords = ivec2(gl_FragCoord.xy);
    uint index;
    Sprite sprite;

    if (!findSprite(spriteCoords, index, sprite)) {
        discard;
    }

    self = lightQuads[index];
    mappedUV = interpolateSprite(sprite, spriteCoords) + 1 / (vec2(sprite.width, sprite.height) * 2);
    antiAliasStep = 1 / (vec2(sprite.width, sprite.height) * 3);
}