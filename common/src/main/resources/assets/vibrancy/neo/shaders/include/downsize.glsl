uniform int DownsizeFactor = 1;

vec2 getScreenUV(vec2 screen) {
    return gl_FragCoord.xy / screen;
}