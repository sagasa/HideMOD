#version 410
// 頂点座標（ローカル座標系）
layout(location = 0) in vec4 in_Position;

uniform vec4 u_dummy;
uniform mat4 u_WorldViewProjectionMat;
uniform mat4 u_ModelViewMatrix;
uniform mat4 u_ProjectionMatrix;
// ワールド・ビュー・プロジェクション変換行列


out vec4 test_color;  

void main(void) {
    // ワールド・ビュー・プロジェクション変換
    mat4 mat = u_ProjectionMatrix*u_ModelViewMatrix;
    gl_Position =u_WorldViewProjectionMat*in_Position;
}