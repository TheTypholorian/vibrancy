#version 450

#include "vibrancy:include/common"

layout(triangles) in;
layout(triangle_strip, max_vertices = 8) out;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 LightPos;
uniform float LightRadius;

in vec2 uv[];

out flat Triangle triangle;

void vertex(vec4 v) {
    gl_Position = v;
    EmitVertex();
}

vec3 interpolateVertex(vec3 v, float len) {
    return v + normalize(v - LightPos) * len;
}

vec4 projectVertex(vec3 v) {
    // TODO why have to multiply by 3?
    vec4 h = ProjMat * ModelViewMat * vec4(v * 3, 1);
    return h;
}

void main() {
    vec3 v0 = gl_in[0].gl_Position.xyz;
    vec3 v1 = gl_in[1].gl_Position.xyz;
    vec3 v2 = gl_in[2].gl_Position.xyz;

    // 0 1 2 X
    // 0 2 1 X
    // 1 0 2 X
    // 2 1 0 X
    // 1 2 0 X
    // 2 0 1 X
    triangle = Triangle(
            v0, v1, v2,
            uv[0], uv[1], uv[2]
    );

    float len = LightRadius;

    vec3 v3 = interpolateVertex(v0, len);
    vec3 v4 = interpolateVertex(v1, len);
    vec3 v5 = interpolateVertex(v2, len);

    vec4 p0 = projectVertex(v0);
    vec4 p1 = projectVertex(v1);
    vec4 p2 = projectVertex(v2);

    vec4 p3 = projectVertex(v3);
    vec4 p4 = projectVertex(v4);
    vec4 p5 = projectVertex(v5);

    vertex(p2);
    vertex(p0);
    vertex(p1);
    vertex(p3);
    vertex(p4);
    vertex(p5);
    vertex(p1);
    vertex(p2);

    EndPrimitive();
}
