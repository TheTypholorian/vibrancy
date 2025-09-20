vec4 ripple(vec4 _min, vec4 _max, float _GameTime, float _dist, float _shineSpeed, float _colorSpeed, float _distShine, float _cScale) {
    float shine = sin(_GameTime * _shineSpeed) * sin(_dist * _distShine) * _cScale / 4;
    return ((_max - _min) * (sin(_GameTime * _colorSpeed + _cScale * 2) / 2 + 0.5 + clamp(shine, -0.5, 0.0)) + _min) * (1 + clamp(shine, 0.0, 0.5));
}

vec4 rippleEnchant(float _GameTime, vec2 _FragTexCoord) {
    vec4 min = vec4(0.17, 0.07, 0.3, 1);
    vec4 max = vec4(0.64, 0.32, 1, 1);
    float d = sqrt(clamp(distance(_FragTexCoord, vec2(0.5, 0.5)) / 0.7, 0, 1));
    return ripple(min, max, _GameTime, d, 4, 3, 0.5, d);
}

vec4 rippleCube(vec4 _min, vec4 _max, float _GameTime, float _dist, vec2 _FragTexCoord) {
    return ripple(_min, _max, _GameTime, _dist, 4, 2, 3, sqrt(clamp(distance(_FragTexCoord, vec2(0.5, 0.5)) / 0.7, 0, 1)));
}