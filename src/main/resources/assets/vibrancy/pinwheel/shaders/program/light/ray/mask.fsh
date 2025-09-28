#version 430

uniform sampler2D DiffuseDepthSampler;
uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    if (abs(texture(DiffuseDepthSampler, gl_FragCoord.xy / ScreenSize).r - gl_FragCoord.z) > 1e-4) {
        discard;
    }
}
