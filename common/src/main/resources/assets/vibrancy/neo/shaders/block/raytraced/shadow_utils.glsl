vec2 directionToShadowCoords(vec3 dir) {
    vec2 uv = vec2(atan(dir.z, dir.x) / 3.1415926535, dir.y);
    uv.x = (uv.x + 1.0) * 0.5;
    uv.y = uv.y * 0.5 + 0.5;
    return uv;
}

vec3 shadowCoordsToDirection(vec2 uv) {
    float angX = (uv.x * 2.0 - 1.0) * 3.1415926535;
    float angY = uv.y * 2.0 - 1.0;
    float cosAng = sqrt(1.0 - angY * angY);
    vec3 dir = vec3(cos(angX) * cosAng, angY, sin(angX) * cosAng);
    return dir;
}