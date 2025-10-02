vec3 hash3(vec3 p) {
    p = vec3(
    dot(p, vec3(127.1, 311.7, 74.7)),
    dot(p, vec3(269.5, 183.3, 246.1)),
    dot(p, vec3(113.5, 271.9, 124.6))
    );
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float fade(float t) {
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float perlin(vec3 p) {
    vec3 pi = floor(p);
    vec3 pf = fract(p);

    vec3 g000 = hash3(pi + vec3(0.0, 0.0, 0.0));
    vec3 g100 = hash3(pi + vec3(1.0, 0.0, 0.0));
    vec3 g010 = hash3(pi + vec3(0.0, 1.0, 0.0));
    vec3 g110 = hash3(pi + vec3(1.0, 1.0, 0.0));
    vec3 g001 = hash3(pi + vec3(0.0, 0.0, 1.0));
    vec3 g101 = hash3(pi + vec3(1.0, 0.0, 1.0));
    vec3 g011 = hash3(pi + vec3(0.0, 1.0, 1.0));
    vec3 g111 = hash3(pi + vec3(1.0, 1.0, 1.0));

    float n000 = dot(g000, pf - vec3(0.0, 0.0, 0.0));
    float n100 = dot(g100, pf - vec3(1.0, 0.0, 0.0));
    float n010 = dot(g010, pf - vec3(0.0, 1.0, 0.0));
    float n110 = dot(g110, pf - vec3(1.0, 1.0, 0.0));
    float n001 = dot(g001, pf - vec3(0.0, 0.0, 1.0));
    float n101 = dot(g101, pf - vec3(1.0, 0.0, 1.0));
    float n011 = dot(g011, pf - vec3(0.0, 1.0, 1.0));
    float n111 = dot(g111, pf - vec3(1.0, 1.0, 1.0));

    vec3 u = vec3(fade(pf.x), fade(pf.y), fade(pf.z));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);

    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);

    return mix(nxy0, nxy1, u.z);
}
