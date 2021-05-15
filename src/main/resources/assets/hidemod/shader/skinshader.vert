#version 430
// 頂点座標（ローカル座標系）
layout(location = 0) in vec4 in_Position;
// 法線ベクトル（ローカル座標系）
layout(location = 1) in vec4 in_Normal;
// テクスチャ座標（ローカル座標系）
layout(location = 2) in vec2 in_TexCoord;

// ボーン番号
layout(location = 5)  in vec4 in_BoneIndices;  
// ボーンウェイト
layout(location = 6)  in vec4 in_BoneWeight;

// ワールド・ビュー・プロジェクション変換行列
uniform mat4 u_WorldViewProjectionMatrix;

// ボーンの変換行列
uniform mat4 u_BoneMatrices[128];     

out vec2 v_TexCoord;
out vec4 test_color;  

void main(void) {
    // スキニング用の変換行列を求める(最大４つのボーンの加重平均を求める）
    mat4 matLocal = u_BoneMatrices[int(in_BoneIndices.x)] * in_BoneWeight.x
                  + u_BoneMatrices[int(in_BoneIndices.y)] * in_BoneWeight.y
                  + u_BoneMatrices[int(in_BoneIndices.z)] * in_BoneWeight.z
                  + u_BoneMatrices[int(in_BoneIndices.w)] * in_BoneWeight.w;
    // スキニング後のローカル座標系に変換
    vec4 localPosition = matLocal * in_Position;
    v_TexCoord = in_TexCoord;
    test_color = in_BoneWeight;
    // ワールド・ビュー・プロジェクション変換
    gl_Position = u_WorldViewProjectionMatrix*localPosition;
}