#version 430

layout(vertices = 4) out;

in vec3 pos_vs[];
out vec3 pos_tc[];

void main() {
    pos_tc[gl_InvocationID] = pos_vs[gl_InvocationID];

    gl_TessLevelInner[0]=4;
    gl_TessLevelInner[1]=2;

    gl_TessLevelOuter[0]=4;
    gl_TessLevelOuter[1]=4;
    gl_TessLevelOuter[2]=2;
    gl_TessLevelOuter[3]=2;
}