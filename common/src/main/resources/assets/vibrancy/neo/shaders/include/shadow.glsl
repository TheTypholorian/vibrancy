vec2 directionToShadowCoords(vec3 dir) {
    vec2 uv = vec2(atan(dir.z, dir.x), asin(dir.y)) / 3.1415926535;
    uv.x = (uv.x + 1) / 2;
    uv.y += 0.5;
    return uv;
}