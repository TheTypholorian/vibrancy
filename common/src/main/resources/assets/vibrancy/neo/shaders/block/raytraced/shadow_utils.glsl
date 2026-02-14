vec2 directionToShadowCoords(vec3 dir, vec2 atlasSize) {
    vec3 absDir = abs(dir);
    uint face;
    vec2 faceUv;

    if (absDir.x >= absDir.y && absDir.x >= absDir.z) {
        if (dir.x > 0.0) {
            face = 0;
            faceUv = vec2(-dir.z, dir.y) / absDir.x;
        } else {
            face = 1;
            faceUv = vec2(dir.z, dir.y) / absDir.x;
        }
    } else if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        if (dir.y > 0.0) {
            face = 2;
            faceUv = vec2(dir.x, -dir.z) / absDir.y;
        } else {
            face = 3;
            faceUv = vec2(dir.x, dir.z) / absDir.y;
        }
    } else {
        if (dir.z > 0.0) {
            face = 4;
            faceUv = vec2(dir.x, dir.y) / absDir.z;
        } else {
            face = 5;
            faceUv = vec2(-dir.x, dir.y) / absDir.z;
        }
    }

    vec2 texelSize = 1.0 / vec2(atlasSize.y, atlasSize.y);
    vec2 inset = texelSize / 2;

    faceUv = faceUv * 0.5 + 0.5;
    faceUv = clamp(faceUv, inset, vec2(1.0) - inset);

    faceUv.x = (faceUv.x + float(face)) / 6.0;
    return faceUv;
}

vec3 shadowCoordsToDirection(vec2 uv) {
    uint face = uint(floor(uv.x * 6.0));
    vec2 faceUv = vec2(fract(uv.x * 6.0), uv.y) * 2.0 - 1.0;

    if (face == 0) return normalize(vec3(1.0, faceUv.y, -faceUv.x));
    if (face == 1) return normalize(vec3(-1.0, faceUv.y, faceUv.x));
    if (face == 2) return normalize(vec3(faceUv.x, 1.0, -faceUv.y));
    if (face == 3) return normalize(vec3(faceUv.x, -1.0, faceUv.y));
    if (face == 4) return normalize(vec3(faceUv.x, faceUv.y, 1.0));
    return normalize(vec3(-faceUv.x, faceUv.y, -1.0));
}

vec2 faceAndDirectionToShadowCoords(vec3 dir, uint face) {
    if (face == 0) return vec2(-dir.z, dir.y) / abs(dir.x);
    if (face == 1) return vec2(dir.z, dir.y) / abs(dir.x);
    if (face == 2) return vec2(dir.x, -dir.z) / abs(dir.y);
    if (face == 3) return vec2(dir.x, dir.z) / abs(dir.y);
    if (face == 4) return vec2(dir.x, dir.y) / abs(dir.z);
    return vec2(-dir.x, dir.y) / abs(dir.z);
}