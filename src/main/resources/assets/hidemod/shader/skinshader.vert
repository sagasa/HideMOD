#version 410
// 頂点座標（ローカル座標系）
layout(location = 0) in vec4 in_Position;

// ボーン番号
layout(location = 5)  in vec4 in_BoneIndices;  
// ボーンウェイト
layout(location = 6)  in vec4 in_BoneWeight;

// ワールド・ビュー・プロジェクション変換行列
uniform mat4 u_WorldViewProjectionMatrix;

uniform mat4 u_Test;

// ボーンの変換行列
uniform mat4 gs_BoneMatrices[128];     

varying vec4 test_color;  

void main(void) {
    // スキニング用の変換行列を求める(最大４つのボーンの加重平均を求める）
    mat4 matLocal = gs_BoneMatrices[int(in_BoneIndices.x)];
    // スキニング後のローカル座標系に変換
    vec4 localPosition = matLocal * in_Position;
    //localPosition = u_Test * in_Position;
    test_color = in_BoneWeight;
    // ワールド・ビュー・プロジェクション変換
    gl_Position = u_WorldViewProjectionMatrix*localPosition;
}