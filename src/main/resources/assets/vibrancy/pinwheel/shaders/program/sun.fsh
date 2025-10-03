#version 150

uniform float SkyAngle;
uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // angle = 1 at time 5999
    // noon is 6000
    // midnight is 18000
    float angleSin = sin(SkyAngle * 2); // 1 at noon, 0 at midnight
    float height = angleSin / 2 + 0.5;
    float d = 1 - clamp(distance(texCoord0, vec2(0)), 0, 1);

    //d * (1 - height) * 2
    fragColor = vec4(1 - height, 0, 0, 1) * ColorModulator;
    //fragColor.r = d * (1 - height);
}
