vec2 directionToShadowCoords(vec3 dir) {
    vec2 uv = vec2(atan(dir.z, dir.x), asin(dir.y)) / 3.1415926535;
    uv.x = (uv.x + 1) / 2;
    uv.y += 0.5;
    return uv;
}

vec3 shadowCoordsToDirection(vec2 uv) {
    vec2 ang = vec2(uv.x * 2.0 - 1.0, uv.y - 0.5) * 3.1415926535;
    float cosAng = cos(ang.y);
    vec3 dir = vec3(cos(ang.x) * cosAng, sin(ang.y), sin(ang.x) * cosAng);
    return dir;
}