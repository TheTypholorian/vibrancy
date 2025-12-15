#version 450

layout(triangles) in;
layout(triangle_strip, max_vertices = 24) out;

void vertex(vec4 v) {
    gl_Position = v;
    EmitVertex();
}

void face(vec4 v0, vec4 v1, vec4 v2, vec4 v3) {
    vertex(v0);
    vertex(v1);
    vertex(v2);
    vertex(v3);

    EndPrimitive();
}

void main() {
    vec4 v0 = gl_in[0].gl_Position;
    vec4 v1 = gl_in[1].gl_Position;
    vec4 v2 = gl_in[2].gl_Position;
    vec4 v3 = gl_in[3].gl_Position;

    face(v3, v0, v2, v1);
}
