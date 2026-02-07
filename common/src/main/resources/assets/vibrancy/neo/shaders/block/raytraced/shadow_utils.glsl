vec2 directionToShadowCoords(vec3 dir, vec2 atlasSize) {
    vec3 absDir = abs(dir);
    float faceIdx;
    vec2 faceUv;

    if (absDir.x >= absDir.y && absDir.x >= absDir.z) {
        if (dir.x > 0.0) {
            faceIdx = 0.0;
            faceUv = vec2(-dir.z, dir.y) / absDir.x;
        } else {
            faceIdx = 1.0;
            faceUv = vec2(dir.z, dir.y) / absDir.x;
        }
    } else if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        if (dir.y > 0.0) {
            faceIdx = 2.0;
            faceUv = vec2(dir.x, -dir.z) / absDir.y;
        } else {
            faceIdx = 3.0;
            faceUv = vec2(dir.x, dir.z) / absDir.y;
        }
    } else {
        if (dir.z > 0.0) {
            faceIdx = 4.0;
            faceUv = vec2(dir.x, dir.y) / absDir.z;
        } else {
            faceIdx = 5.0;
            faceUv = vec2(-dir.x, dir.y) / absDir.z;
        }
    }

    vec2 texelSize = 1.0 / vec2(atlasSize.y, atlasSize.y);
    vec2 inset = texelSize / 2;

    faceUv = faceUv * 0.5 + 0.5;
    faceUv = clamp(faceUv, inset, vec2(1.0) - inset);

    faceUv.x = (faceUv.x + faceIdx) / 6.0;
    return faceUv;
}

vec3 shadowCoordsToDirection(vec2 uv) {
    float faceIdx = floor(uv.x * 6.0);
    vec2 faceUv = vec2(fract(uv.x * 6.0), uv.y) * 2.0 - 1.0;

    if (faceIdx == 0.0) return normalize(vec3(1.0, faceUv.y, -faceUv.x));
    if (faceIdx == 1.0) return normalize(vec3(-1.0, faceUv.y, faceUv.x));
    if (faceIdx == 2.0) return normalize(vec3(faceUv.x, 1.0, -faceUv.y));
    if (faceIdx == 3.0) return normalize(vec3(faceUv.x, -1.0, faceUv.y));
    if (faceIdx == 4.0) return normalize(vec3(faceUv.x, faceUv.y, 1.0));
    return normalize(vec3(-faceUv.x, faceUv.y, -1.0));
}