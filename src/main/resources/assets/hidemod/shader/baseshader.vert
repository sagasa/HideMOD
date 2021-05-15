#version 430
// 頂点座標（ローカル座標系）
layout(location = 0) in vec4 in_Position;
// 法線ベクトル（ローカル座標系）
layout(location = 1) in vec4 in_Normal;
// テクスチャ座標（ローカル座標系）
layout(location = 2) in vec2 in_TexCoord;

// ワールド・ビュー・プロジェクション変換行列
uniform mat4 u_WorldViewProjectionMatrix;

out vec2 v_TexCoord;
out vec4 test_color;  

void main(void) {
    // スキニング後のローカル座標系に変換
    vec4 localPosition = in_Position;
    v_TexCoord = in_TexCoord;
    // ワールド・ビュー・プロジェクション変換
    gl_Position = u_WorldViewProjectionMatrix*localPosition;
}