vec3 getShadowPosition(ivec2 textureSize, vec2 texCoord, vec3 pos) {
    if (config.visuals.alignPixels) {
        vec2 texelPos = texCoord * textureSize;
        vec2 d = (floor(texelPos) + 0.5 - texelPos) / textureSize;

        vec2 stepA = dFdx(texCoord);
        vec2 stepB = dFdy(texCoord);
        float det = stepA.x * stepB.y - stepA.y * stepB.x;
        float a = (d.x * stepB.y - d.y * stepB.x) / det;
        float b = (-d.x * stepA.y + d.y * stepA.x) / det;

        return pos + dFdx(pos) * a + dFdy(pos) * b;
    } else {
        return pos;
    }
}