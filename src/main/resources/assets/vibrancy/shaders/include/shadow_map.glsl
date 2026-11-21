// [-1, 1]
vec2 directionToShadowCoords(vec3 dir, out uint face) {
    vec3 absDir = abs(dir);
    vec2 faceUv;

    if (absDir.x >= absDir.y && absDir.x >= absDir.z) {
        if (dir.x > 0.0) {
            face = 0u;
            faceUv = vec2(-dir.z, dir.y) / dir.x;
        } else {
            face = 1u;
            faceUv = vec2(dir.z, dir.y) / -dir.x;
        }
    } else if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        if (dir.y > 0.0) {
            face = 2u;
            faceUv = vec2(dir.x, -dir.z) / dir.y;
        } else {
            face = 3u;
            faceUv = vec2(dir.x, dir.z) / -dir.y;
        }
    } else {
        if (dir.z > 0.0) {
            face = 4u;
            faceUv = vec2(dir.x, dir.y) / dir.z;
        } else {
            face = 5u;
            faceUv = vec2(-dir.x, dir.y) / -dir.z;
        }
    }

    return faceUv;
}

vec3 faceAndShadowCoordsToDirection(vec2 uv, uint face) {
    vec2 faceUv = vec2(fract(uv.x * 6.0), uv.y) * 2.0 - 1.0;
    if (face == 0u) return normalize(vec3(1.0, faceUv.y, -faceUv.x));
    if (face == 1u) return normalize(vec3(-1.0, faceUv.y, faceUv.x));
    if (face == 2u) return normalize(vec3(faceUv.x, 1.0, -faceUv.y));
    if (face == 3u) return normalize(vec3(faceUv.x, -1.0, faceUv.y));
    if (face == 4u) return normalize(vec3(faceUv.x, faceUv.y, 1.0));
    return normalize(vec3(-faceUv.x, faceUv.y, -1.0));
}

float faceAndDirectionToDistance(vec3 dir, uint face) {
    if (face == 0u) return dir.x;
    if (face == 1u) return -dir.x;
    if (face == 2u) return dir.y;
    if (face == 3u) return -dir.y;
    if (face == 4u) return dir.z;
    return -dir.z;
}

// [-1, 1]
vec2 faceAndDirectionToShadowCoords(vec3 dir, uint face) {
    if (face == 0u) return vec2(-dir.z, dir.y) / dir.x;
    if (face == 1u) return vec2(dir.z, dir.y) / -dir.x;
    if (face == 2u) return vec2(dir.x, -dir.z) / dir.y;
    if (face == 3u) return vec2(dir.x, dir.z) / -dir.y;
    if (face == 4u) return vec2(dir.x, dir.y) / dir.z;
    return vec2(-dir.x, dir.y) / -dir.z;
}