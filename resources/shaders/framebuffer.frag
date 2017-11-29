#version 330

in vec2 v_texCoord0;

out vec4 o_fragColor;

uniform sampler2D u_texture;

void main() {
    o_fragColor = texture(u_texture, v_texCoord0);
}