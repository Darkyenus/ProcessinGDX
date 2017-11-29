#version 330

in vec2 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

out vec4 v_color;
out vec2 v_texCoord0;

uniform mat4 u_projTrans;

void main() {
    v_color = a_color;
    v_texCoord0 = a_texCoord0;

    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
