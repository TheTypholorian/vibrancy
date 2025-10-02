#version 430

#include "veil:fog"

uniform float SkyAngle;
uniform vec3 SkyColor;
uniform vec4 FogColor = vec4(1, 0, 0, 1);

in vec3 pos;

out vec4 fragColor;

void main() {
    // angle = 1 at time 5999
    // noon is 6000
    // midnight is 18000
    float angleSin = sin((SkyAngle + 0.25) * 3.14159 * 2); // 1 at noon, 0 at midnight
    float brightness = (angleSin * angleSin * angleSin) / 2 + 0.5;
    float height = angleSin / 2 + 0.5;
    fragColor = FogColor;//vec4(mix(SkyColor, FogColor.rgb, 0.5), 1);
}
