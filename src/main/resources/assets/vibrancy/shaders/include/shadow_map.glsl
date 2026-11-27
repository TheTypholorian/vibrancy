// [-1, 1]
vec2 directionToShadowCoords(vec3 dir, out uint face) {
    vec3 absDir = abs(dir);
    vec2 coords;

    if (absDir.x >= absDir.y && absDir.x >= absDir.z) {
        if (dir.x > 0.0) {
            face = 0u;
            coords = vec2(-dir.z, dir.y) / dir.x;
        } else {
            face = 1u;
            coords = vec2(dir.z, dir.y) / -dir.x;
        }
    } else if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        if (dir.y > 0.0) {
            face = 2u;
            coords = vec2(dir.x, -dir.z) / dir.y;
        } else {
            face = 3u;
            coords = vec2(dir.x, dir.z) / -dir.y;
        }
    } else {
        if (dir.z > 0.0) {
            face = 4u;
            coords = vec2(dir.x, dir.y) / dir.z;
        } else {
            face = 5u;
            coords = vec2(-dir.x, dir.y) / -dir.z;
        }
    }

    return coords;
}

float faceAndDirectionToDistance(vec3 dir, uint face) {
    if (face == 0u) return dir.x;
    if (face == 1u) return -dir.x;
    if (face == 2u) return dir.y;
    if (face == 3u) return -dir.y;
    if (face == 4u) return dir.z;
    return -dir.z;
}

// Returns [-1, 1]
vec3 faceAndDirectionToShadowCoords(vec3 dir, uint face, float radius) {
    if (face == 0u) return vec3(-dir.z / dir.x, dir.y / dir.x, dir.x / radius);
    if (face == 1u) return vec3(dir.z / -dir.x, dir.y / -dir.x, -dir.x / radius);
    if (face == 2u) return vec3(dir.x / dir.y, -dir.z / dir.y, dir.y / radius);
    if (face == 3u) return vec3(dir.x / -dir.y, dir.z / -dir.y, -dir.y / radius);
    if (face == 4u) return vec3(dir.x / dir.z, dir.y / dir.z, dir.z / radius);
    return vec3(-dir.x / -dir.z, dir.y / -dir.z, -dir.z / radius);
}

// Takes [-1, 1], returns [-1, 1]
vec2 faceRelativeToAbsolute(vec2 coords, uint face) {
    return vec2((coords.x + 1.0 + 2.0 * float(face)) / 6.0 - 1.0, coords.y);
}

// Takes [0, 1], returns [0, 1]
vec2 absoluteToFaceRelative(vec2 coords, out uint face) {
    float x = coords.x * 6.0;
    face = uint(floor(x));
    return vec2(fract(x), coords.y);
}

// Takes [-1, 1], returns [-1, 1]
vec3 skyLightFisheye(vec3 pos) {
    float power = 4.0; // TODO
    // TODO optimize this
    return sign(pos) * atan(abs(pos) * power) / atan(power);
}