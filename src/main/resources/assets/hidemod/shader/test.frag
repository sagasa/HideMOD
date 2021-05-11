#version 330
// 出力カラー
layout(location = 0) out vec4 out_FragColor;

in vec4 test_color;

void main(void) {
    // 緑色を出力(RGBA)
    out_FragColor = vec4(0.0, 1.0, 1.0, 1.0);
}