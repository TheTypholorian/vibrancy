struct Sprite {
    int x;
    int y;
    int width;
    int height;
};

bool spriteContains(Sprite tex, ivec2 vec) {
    return vec.x >= tex.x && vec.y >= tex.y && vec.x < (tex.x + tex.width) && vec.y < (tex.y + tex.height);
}

vec2 interpolateSprite(Sprite tex, ivec2 vec) {
    return vec2(vec - ivec2(tex.x, tex.y)) / vec2(tex.width, tex.height);
}