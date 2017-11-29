#version 330

in vec2 a_position;
in vec4 a_color;
in vec2 a_texCoord0;

out vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_texCoord0;

    gl_Position = vec4(a_position, 0.0, 1.0);
}
