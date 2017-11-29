#version 330

in vec4 v_color;
in vec2 v_texCoord0;

out vec4 o_fragColor;

uniform sampler2D u_texture;

void main() {
	vec4 texColor = texture(u_texture, v_texCoord0);

    o_fragColor = texColor * v_color;
}