#version 330
// 出力カラー
layout(location = 0) out vec4 out_FragColor;

uniform sampler2D u_baseColorTexture;
uniform sampler2D u_normalTexture;
uniform sampler2D u_occlusionTexture;
uniform sampler2D u_emissiveTexture;

uniform int u_hasNormalTexture;
uniform int u_hasOcclusionTexture;
uniform int u_hasEmissiveTexture;

in vec2 v_TexCoord;
in vec4 test_color;
in vec4 v_norm;

void main(void) {
	// 緑色を出力(RGBA)
	//out_FragColor = texture2D(u_baseColorTexture, v_TexCoord);

	float light = dot(v_norm, vec4(0.0, -1.0, 0.0, 0.0)) / 2.3 + 0.56;
	out_FragColor = texture2D(u_baseColorTexture, v_TexCoord)
			* vec4(light, light, light, 1);
//out_FragColor = vec4(0.0, 1.0, 1.0, 0.4);
}
