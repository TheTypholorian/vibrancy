struct VisualsConfig {
    bool limitBrightness;
    bool alignPixels;
    float rayBrightness;
    float subtleBrightness;
    float beamBrightness;
    float skyBrightness;
};

struct SpecularConfig {
    bool enabled;
    float strength;
    float exponent;
};

layout(std140) uniform u_VibrancyConfig {
    VisualsConfig visuals;
    SpecularConfig specular;
} config;