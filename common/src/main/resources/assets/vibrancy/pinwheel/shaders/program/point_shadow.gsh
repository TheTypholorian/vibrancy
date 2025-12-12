#version 450

layout(triangles) in;
layout(triangle_strip, max_vertices = 24) out;

in vec3 pos_te[];

void main() {
    vec3 v0 = pos_te[0];
    vec3 v1 = pos_te[1];
    vec3 v2 = pos_te[2];
    vec3 v3 = pos_te[3];
    /*
    vec3 v4 = pos_te[4];
    vec3 v5 = pos_te[5];
    vec3 v6 = pos_te[6];
    vec3 v7 = pos_te[7];
    */

    vec3 q[6] = vec3[6](
    v0, v1, v2,
    v2, v3, v0
    );
/*
    v0, v4, v1,
    v4, v5, v1,
    v1, v5, v2,
    v5, v6, v2,
    v2, v6, v3,
    v6, v7, v3
*/

    for (int i = 0; i < 6; i++) {
        gl_Position = vec4(q[i], 1.0);
        EmitVertex();
    }

    EndPrimitive();
}
